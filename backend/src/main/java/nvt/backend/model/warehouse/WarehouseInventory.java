package nvt.backend.model.warehouse;

import jakarta.persistence.*;
import lombok.Data;
import nvt.backend.model.product.Product;

import java.time.LocalDateTime;

@Entity
@Table(name = "warehouse_inventory", indexes = {
    @Index(name = "idx_inventory_warehouse", columnList = "warehouse_id"),
    @Index(name = "idx_inventory_sector", columnList = "sector_id"),
    @Index(name = "idx_inventory_product", columnList = "product_id"),
    @Index(name = "idx_inventory_warehouse_product", columnList = "warehouse_id, product_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_warehouse_sector_product", columnNames = {"warehouse_id", "sector_id", "product_id"})
})
@Data
public class WarehouseInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id", nullable = false)
    private WarehouseSector sector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity = 0;

    @Column(nullable = false)
    private Integer reservedQuantity = 0; // Rezervisano za porudžbine u obradi

    private LocalDateTime lastUpdated = LocalDateTime.now();

    @Version
    private Long version; // Optimistic locking za konkurentne transakcije

    // Dostupna količina = quantity - reservedQuantity
    public Integer getAvailableQuantity() {
        return quantity - reservedQuantity;
    }
}
