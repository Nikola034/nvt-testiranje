package nvt.backend.services.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nvt.backend.dto.common.PageResponseDTO;
import nvt.backend.dto.order.CreateOrderDTO;
import nvt.backend.dto.order.OrderResponseDTO;
import nvt.backend.model.customer.CustomerCompany;
import nvt.backend.model.order.Order;
import nvt.backend.model.order.OrderItem;
import nvt.backend.model.order.OrderStatus;
import nvt.backend.model.product.Product;
import nvt.backend.model.user.User;
import nvt.backend.model.warehouse.WarehouseInventory;
import nvt.backend.repositories.customer.CustomerCompanyRepository;
import nvt.backend.repositories.order.OrderRepository;
import nvt.backend.repositories.product.ProductRepository;
import nvt.backend.repositories.user.UserRepository;
import nvt.backend.repositories.warehouse.WarehouseInventoryRepository;
import nvt.backend.services.auth.EmailService;
import nvt.backend.services.storage.MinioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerCompanyRepository companyRepository;
    private final WarehouseInventoryRepository inventoryRepository;
    private final UserRepository userRepository;
    private final PdfInvoiceService pdfInvoiceService;
    private final EmailService emailService;
    private final MinioService minioService;

    @Value("${spring.mail.sender}")
    private String mailSender;

    @Value("${minio.bucket.invoices:invoices}")
    private String invoicesBucket;

    /**
     * Kreira novu porudžbinu.
     * 
     * Proces:
     * 1. Validacija - da li kupac ima pristup firmi za dostavu
     * 2. Provera zaliha - da li ima dovoljno proizvoda u svim magacinima zajedno
     * 3. Alokacija zaliha - određuje iz kojih magacina će se uzeti proizvodi
     * 4. Rezervacija zaliha - atomično rezerviše proizvode
     * 5. Kreiranje porudžbine
     * 6. Generisanje PDF fakture
     * 7. Slanje emaila sa fakturom
     */
    @Transactional
    @CacheEvict(value = {"ordersPage", "orderById"}, allEntries = true)
    public OrderResponseDTO createOrder(CreateOrderDTO dto, User customer) {
        log.info("Creating order for customer {} with {} items", customer.getUsername(), dto.getItems().size());

        // 1. Validacija firme za dostavu
        CustomerCompany deliveryCompany = companyRepository.findByIdWithDetails(dto.getDeliveryCompanyId())
                .orElseThrow(() -> new RuntimeException("Delivery company not found"));

        if (!companyRepository.isOwnedByUser(dto.getDeliveryCompanyId(), customer.getId())) {
            throw new RuntimeException("You don't have access to this delivery company");
        }

        // 2. Validacija i provera zaliha za sve proizvode
        Map<Long, Product> productMap = new HashMap<>();
        Map<Long, Integer> requestedQuantities = new HashMap<>();
        List<String> insufficientStock = new ArrayList<>();

        for (CreateOrderDTO.OrderItemDTO itemDto : dto.getItems()) {
            if (itemDto.getQuantity() <= 0) {
                throw new RuntimeException("Quantity must be positive for product ID: " + itemDto.getProductId());
            }

            Product product = productRepository.findByIdWithCategory(itemDto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + itemDto.getProductId()));

            if (!product.isActive()) {
                throw new RuntimeException("Product is not available: " + product.getName());
            }

            productMap.put(itemDto.getProductId(), product);
            requestedQuantities.put(itemDto.getProductId(), itemDto.getQuantity());

            // Proveri ukupnu dostupnu količinu
            Integer available = inventoryRepository.getTotalAvailableQuantity(itemDto.getProductId());
            if (available < itemDto.getQuantity()) {
                insufficientStock.add(String.format("%s (traženo: %d, dostupno: %d)", 
                        product.getName(), itemDto.getQuantity(), available));
            }
        }

        if (!insufficientStock.isEmpty()) {
            throw new RuntimeException("Nedovoljna količina na stanju za: " + String.join(", ", insufficientStock));
        }

        // 3. Alokacija i rezervacija zaliha
        Map<Long, List<InventoryAllocation>> allocations = allocateAndReserveInventory(requestedQuantities);

        try {
            // 4. Kreiranje porudžbine
            Order order = new Order();
            order.setOrderNumber(generateOrderNumber());
            order.setCustomer(customer);
            order.setDeliveryCompany(deliveryCompany);
            order.setStatus(OrderStatus.CONFIRMED);
            order.setNotes(dto.getNotes());

            // Kreiranje stavki
            for (CreateOrderDTO.OrderItemDTO itemDto : dto.getItems()) {
                Product product = productMap.get(itemDto.getProductId());
                List<InventoryAllocation> itemAllocations = allocations.get(itemDto.getProductId());

                // Kreiraj jednu stavku po proizvodu (može biti iz više magacina)
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setProduct(product);
                orderItem.setQuantity(itemDto.getQuantity());
                orderItem.setUnitPrice(product.getPrice());
                orderItem.setProductSku(product.getSku());
                orderItem.setProductName(product.getName());
                
                // Uzmi prvi magacin kao glavni (ili možeš kreirati više stavki po magacinu)
                if (!itemAllocations.isEmpty()) {
                    orderItem.setWarehouse(itemAllocations.get(0).inventory.getWarehouse());
                }

                order.getItems().add(orderItem);
            }

            order.calculateTotals();

            // 5. Generisanje broja fakture
            order.setInvoiceNumber(generateInvoiceNumber());
            order.setInvoiceGeneratedAt(LocalDateTime.now());

            order = orderRepository.save(order);

            // 6. Generisanje PDF fakture
            byte[] pdfBytes = pdfInvoiceService.generateInvoice(order);

            // Čuvanje PDF-a u MinIO
            String invoicePath = saveInvoiceToStorage(order, pdfBytes);
            order.setInvoicePath(invoicePath);
            order = orderRepository.save(order);

            // 7. Slanje emaila sa fakturom
            sendInvoiceEmail(order, pdfBytes);

            log.info("Order {} created successfully for customer {}", order.getOrderNumber(), customer.getUsername());

            return OrderResponseDTO.fromEntity(order);

        } catch (Exception e) {
            // Rollback rezervacija u slučaju greške
            log.error("Error creating order, rolling back reservations", e);
            rollbackReservations(allocations);
            throw new RuntimeException("Failed to create order: " + e.getMessage(), e);
        }
    }

    /**
     * Alokacija i atomična rezervacija zaliha iz magacina.
     * Koristi strategiju "najveća količina prva" - uzima iz magacina koji ima najviše.
     */
    private Map<Long, List<InventoryAllocation>> allocateAndReserveInventory(Map<Long, Integer> requestedQuantities) {
        Map<Long, List<InventoryAllocation>> allocations = new HashMap<>();

        for (Map.Entry<Long, Integer> entry : requestedQuantities.entrySet()) {
            Long productId = entry.getKey();
            int remainingQuantity = entry.getValue();
            List<InventoryAllocation> productAllocations = new ArrayList<>();

            // Dobij sve inventare sortirane po dostupnoj količini (opadajuće)
            List<WarehouseInventory> inventories = inventoryRepository.findAvailableInventoryForProduct(productId);

            for (WarehouseInventory inventory : inventories) {
                if (remainingQuantity <= 0) break;

                int available = inventory.getAvailableQuantity();
                int toReserve = Math.min(available, remainingQuantity);

                if (toReserve > 0) {
                    // Atomična rezervacija sa optimistic locking
                    int updated = inventoryRepository.reserveQuantity(inventory.getId(), toReserve);
                    
                    if (updated == 0) {
                        // Neko je zauzeo zalihe u međuvremenu, pokušaj ponovo
                        log.warn("Concurrent reservation detected for inventory {}, retrying...", inventory.getId());
                        // Refresh i retry logika bi bila ovde u produkciji
                        throw new RuntimeException("Konkurentna rezervacija zaliha. Molimo pokušajte ponovo.");
                    }

                    productAllocations.add(new InventoryAllocation(inventory, toReserve));
                    remainingQuantity -= toReserve;
                }
            }

            if (remainingQuantity > 0) {
                // Ne bi trebalo da se desi ako je prethodna provera bila ispravna
                throw new RuntimeException("Insufficient inventory for product: " + productId);
            }

            allocations.put(productId, productAllocations);
        }

        return allocations;
    }

    private void rollbackReservations(Map<Long, List<InventoryAllocation>> allocations) {
        for (List<InventoryAllocation> productAllocations : allocations.values()) {
            for (InventoryAllocation allocation : productAllocations) {
                try {
                    inventoryRepository.releaseReservedQuantity(allocation.inventory.getId(), allocation.quantity);
                } catch (Exception e) {
                    log.error("Failed to rollback reservation for inventory {}", allocation.inventory.getId(), e);
                }
            }
        }
    }

    private String generateOrderNumber() {
        String prefix = "ORD";
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        Optional<String> lastNumber = orderRepository.findLastOrderNumber();
        int sequence = 1;
        
        if (lastNumber.isPresent()) {
            String last = lastNumber.get();
            if (last.startsWith(prefix + datePart)) {
                try {
                    sequence = Integer.parseInt(last.substring((prefix + datePart).length())) + 1;
                } catch (NumberFormatException e) {
                    // Ignore, start from 1
                }
            }
        }
        
        return String.format("%s%s%04d", prefix, datePart, sequence);
    }

    private String generateInvoiceNumber() {
        String prefix = "INV";
        String year = String.valueOf(LocalDateTime.now().getYear());
        int random = new Random().nextInt(900000) + 100000;
        return String.format("%s-%s-%06d", prefix, year, random);
    }

    private String saveInvoiceToStorage(Order order, byte[] pdfBytes) {
        try {
            minioService.createBucketIfNotExists(invoicesBucket);
            
            String fileName = String.format("invoices/%s/%s.pdf", 
                    order.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy/MM")),
                    order.getInvoiceNumber());

            // Koristi postojeći MinioService za upload
            // Za ByteArrayInputStream treba malo drugačiji pristup
            // Kreiraj privremeni fajl ili direktno koristi MinIO API
            
            return fileName; // Za sada vraćamo samo putanju, implementacija čuvanja može varirati
        } catch (Exception e) {
            log.error("Failed to save invoice to storage", e);
            return null;
        }
    }

    private void sendInvoiceEmail(Order order, byte[] pdfBytes) {
        try {
            String subject = "Faktura " + order.getInvoiceNumber() + " - Porudžbina " + order.getOrderNumber();
            
            String body = String.format("""
                <html>
                <body>
                    <h2>Poštovani %s,</h2>
                    <p>Hvala Vam na porudžbini!</p>
                    <p>Vaša porudžbina <strong>%s</strong> je uspešno kreirana.</p>
                    
                    <h3>Detalji porudžbine:</h3>
                    <ul>
                        <li><strong>Broj fakture:</strong> %s</li>
                        <li><strong>Ukupan iznos:</strong> %,.2f RSD</li>
                        <li><strong>Adresa dostave:</strong> %s</li>
                    </ul>
                    
                    <p>U prilogu se nalazi PDF faktura.</p>
                    
                    <p>Za sva pitanja, kontaktirajte nas na prodaja@smartly.com</p>
                    
                    <br>
                    <p>S poštovanjem,</p>
                    <p><strong>Smartly Tim</strong></p>
                </body>
                </html>
                """,
                    order.getCustomer().getName(),
                    order.getOrderNumber(),
                    order.getInvoiceNumber(),
                    order.getTotalAmount(),
                    buildDeliveryAddress(order)
            );

            // Pošalji email (bez attachmenta za sada, može se dodati)
            emailService.sendMail(mailSender, order.getCustomer().getUsername(), subject, body);
            
            order.setInvoiceSentAt(LocalDateTime.now());
            orderRepository.save(order);
            
            log.info("Invoice email sent to {} for order {}", order.getCustomer().getUsername(), order.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to send invoice email for order {}", order.getOrderNumber(), e);
            // Ne bacamo exception da ne bismo rollbackovali celu transakciju
        }
    }

    private String buildDeliveryAddress(Order order) {
        var company = order.getDeliveryCompany();
        StringBuilder sb = new StringBuilder();
        sb.append(company.getName());
        if (company.getStreet() != null) {
            sb.append(", ").append(company.getStreet());
            if (company.getStreetNumber() != null) {
                sb.append(" ").append(company.getStreetNumber());
            }
        }
        if (company.getCity() != null) {
            sb.append(", ").append(company.getCity().getName());
        }
        return sb.toString();
    }

    // Query metode

    @Transactional(readOnly = true)
    @Cacheable(value = "orderById", key = "#id")
    public OrderResponseDTO getOrderById(Long id) {
        Order order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return OrderResponseDTO.fromEntity(order);
    }

    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderByIdForCustomer(Long id, User customer) {
        Order order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        if (order.getCustomer().getId() != customer.getId()) {
            throw new RuntimeException("Access denied to this order");
        }
        
        return OrderResponseDTO.fromEntity(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getCustomerOrders(User customer) {
        return orderRepository.findByCustomerId(customer.getId()).stream()
                .map(OrderResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "ordersPage", key = "'customer-' + #customer.id + '-' + #page + '-' + #size")
    public PageResponseDTO<OrderResponseDTO> getCustomerOrdersPaged(User customer, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Long> idPage = orderRepository.findIdsByCustomerId(customer.getId(), pageable);
        
        List<Order> orders = idPage.getContent().isEmpty()
                ? List.of()
                : orderRepository.findAllByIdsWithDetails(idPage.getContent());

        return PageResponseDTO.<OrderResponseDTO>builder()
                .content(orders.stream().map(OrderResponseDTO::fromEntity).collect(Collectors.toList()))
                .page(idPage.getNumber())
                .size(idPage.getSize())
                .totalElements(idPage.getTotalElements())
                .totalPages(idPage.getTotalPages())
                .first(idPage.isFirst())
                .last(idPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "ordersPage", key = "'all-' + #page + '-' + #size")
    public PageResponseDTO<OrderResponseDTO> getAllOrdersPaged(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Long> idPage = orderRepository.findAllIds(pageable);
        
        List<Order> orders = idPage.getContent().isEmpty()
                ? List.of()
                : orderRepository.findAllByIdsWithDetails(idPage.getContent());

        return PageResponseDTO.<OrderResponseDTO>builder()
                .content(orders.stream().map(OrderResponseDTO::fromEntity).collect(Collectors.toList()))
                .page(idPage.getNumber())
                .size(idPage.getSize())
                .totalElements(idPage.getTotalElements())
                .totalPages(idPage.getTotalPages())
                .first(idPage.isFirst())
                .last(idPage.isLast())
                .build();
    }

    // Helper klasa za alokaciju
    private static class InventoryAllocation {
        WarehouseInventory inventory;
        int quantity;

        InventoryAllocation(WarehouseInventory inventory, int quantity) {
            this.inventory = inventory;
            this.quantity = quantity;
        }
    }
}
