package nvt.backend.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nvt.backend.model.order.Order;
import nvt.backend.model.order.OrderItem;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO implements Serializable {
    private Long id;
    private String orderNumber;
    private String status;
    
    // Customer info
    private Long customerId;
    private String customerName;
    private String customerEmail;
    
    // Delivery company info
    private Long deliveryCompanyId;
    private String deliveryCompanyName;
    private String deliveryAddress;
    
    // Items
    private List<OrderItemResponseDTO> items;
    
    // Amounts
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    
    private String notes;
    
    // Invoice
    private String invoiceNumber;
    private LocalDateTime invoiceGeneratedAt;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponseDTO implements Serializable {
        private Long id;
        private Long productId;
        private String productSku;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
        private String warehouseName;
    }

    public static OrderResponseDTO fromEntity(Order order) {
        return OrderResponseDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .customerId((long) order.getCustomer().getId())
                .customerName(order.getCustomer().getName() + " " + order.getCustomer().getSurname())
                .customerEmail(order.getCustomer().getUsername())
                .deliveryCompanyId(order.getDeliveryCompany().getId())
                .deliveryCompanyName(order.getDeliveryCompany().getName())
                .deliveryAddress(buildDeliveryAddress(order))
                .items(order.getItems().stream().map(OrderResponseDTO::mapItem).toList())
                .subtotal(order.getSubtotal())
                .taxAmount(order.getTaxAmount())
                .totalAmount(order.getTotalAmount())
                .notes(order.getNotes())
                .invoiceNumber(order.getInvoiceNumber())
                .invoiceGeneratedAt(order.getInvoiceGeneratedAt())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .completedAt(order.getCompletedAt())
                .build();
    }

    private static String buildDeliveryAddress(Order order) {
        var company = order.getDeliveryCompany();
        StringBuilder sb = new StringBuilder();
        if (company.getStreet() != null) {
            sb.append(company.getStreet());
            if (company.getStreetNumber() != null) {
                sb.append(" ").append(company.getStreetNumber());
            }
        }
        if (company.getCity() != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(company.getCity().getName());
        }
        if (company.getCountry() != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(company.getCountry().getName());
        }
        return sb.toString();
    }

    private static OrderItemResponseDTO mapItem(OrderItem item) {
        return OrderItemResponseDTO.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productSku(item.getProductSku())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .lineTotal(item.getLineTotal())
                .warehouseName(item.getWarehouse() != null ? item.getWarehouse().getName() : null)
                .build();
    }
}
