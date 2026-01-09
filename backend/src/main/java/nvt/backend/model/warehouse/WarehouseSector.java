package nvt.backend.model.warehouse;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "warehouse_sectors", indexes = {
    @Index(name = "idx_sector_warehouse", columnList = "warehouse_id"),
    @Index(name = "idx_sector_name", columnList = "name")
})
@Data
public class WarehouseSector {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    // Klimatski uslovi
    private Double targetTemperature; // Ciljna temperatura u Celzijusima
    private Double minTemperature;
    private Double maxTemperature;
    private Double currentTemperature; // Trenutna temperatura (od simulatora)

    private Double targetHumidity; // Ciljna vlažnost u %
    private Double currentHumidity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Warehouse warehouse;

    private boolean active = true;

    private LocalDateTime lastTelemetryUpdate;
    private LocalDateTime createdAt = LocalDateTime.now();
}
