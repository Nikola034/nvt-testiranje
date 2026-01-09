package nvt.backend.model.warehouse;

import jakarta.persistence.*;
import lombok.Data;
import nvt.backend.model.common.City;
import nvt.backend.model.common.Country;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "warehouses", indexes = {
    @Index(name = "idx_warehouse_name", columnList = "name"),
    @Index(name = "idx_warehouse_country", columnList = "country_id"),
    @Index(name = "idx_warehouse_city", columnList = "city_id")
})
@Data
public class Warehouse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String code; // Jedinstveni kod magacina

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id")
    private Country country;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private City city;

    private String street;
    private String streetNumber;
    private Double latitude;
    private Double longitude;

    private boolean active = true;

    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<WarehouseSector> sectors = new HashSet<>();

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;

    // Za praćenje online statusa simulatora
    private LocalDateTime lastHeartbeat;
    private boolean online = false;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
