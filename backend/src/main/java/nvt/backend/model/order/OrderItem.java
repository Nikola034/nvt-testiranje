package nvt.backend.model.order;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import nvt.backend.model.product.Product;
import nvt.backend.model.warehouse.Warehouse;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items", indexes = {
    @Index(name = "idx_order_item_order", columnList = "order_id"),
    @Index(name = "idx_order_item_product", columnList = "product_id"),
    @Index(name = "idx_order_item_warehouse", columnList = "warehouse_id")
})
@Data
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Magacin iz kojeg će se isporučiti (može biti null dok se ne dodeli)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice; // Cena po komadu u momentu poručivanja

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal; // quantity * unitPrice

    // Snapshot podataka o proizvodu u momentu narudžbine
    private String productSku;
    private String productName;

    @PrePersist
    @PreUpdate
    protected void calculateLineTotal() {
        if (quantity != null && unitPrice != null) {
            this.lineTotal = unitPrice.multiply(new BigDecimal(quantity));
        }
    }
}
