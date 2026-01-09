package nvt.backend.controllers.order;

import lombok.RequiredArgsConstructor;
import nvt.backend.config.SecurityUser;
import nvt.backend.dto.common.PageResponseDTO;
import nvt.backend.dto.order.CreateOrderDTO;
import nvt.backend.dto.order.OrderResponseDTO;
import nvt.backend.model.user.User;
import nvt.backend.repositories.user.UserRepository;
import nvt.backend.services.order.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    /**
     * Kreiranje nove porudžbine
     * 
     * Kupac bira proizvode i količine, kao i firmu za dostavu.
     * Sistem proverava da li ima dovoljno proizvoda na stanju,
     * rezerviše količine, kreira porudžbinu, generiše PDF fakturu
     * i šalje je na email kupca.
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> createOrder(
            @RequestBody CreateOrderDTO dto,
            @AuthenticationPrincipal SecurityUser securityUser) {
        try {
            User customer = userRepository.findByUsername(securityUser.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            OrderResponseDTO order = orderService.createOrder(dto, customer);
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Dohvata porudžbinu po ID-u (za kupca)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> getOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUser securityUser) {
        try {
            User user = userRepository.findByUsername(securityUser.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Menadžeri i admini mogu da vide sve porudžbine
            if (user.getAuthorities().contains("MANAGER") || user.getAuthorities().contains("ADMIN")) {
                return ResponseEntity.ok(orderService.getOrderById(id));
            }

            // Kupci mogu da vide samo svoje porudžbine
            return ResponseEntity.ok(orderService.getOrderByIdForCustomer(id, user));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Dohvata sve porudžbine kupca
     */
    @GetMapping("/my")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<OrderResponseDTO>> getMyOrders(
            @AuthenticationPrincipal SecurityUser securityUser) {
        User customer = userRepository.findByUsername(securityUser.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return ResponseEntity.ok(orderService.getCustomerOrders(customer));
    }

    /**
     * Dohvata porudžbine kupca sa paginacijom
     */
    @GetMapping("/my/paged")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<PageResponseDTO<OrderResponseDTO>> getMyOrdersPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal SecurityUser securityUser) {
        User customer = userRepository.findByUsername(securityUser.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return ResponseEntity.ok(orderService.getCustomerOrdersPaged(customer, page, size));
    }

    /**
     * Dohvata sve porudžbine (samo za menadžere)
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<PageResponseDTO<OrderResponseDTO>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(orderService.getAllOrdersPaged(page, size));
    }
}
