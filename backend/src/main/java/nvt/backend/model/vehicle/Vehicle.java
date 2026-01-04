package nvt.backend.model.vehicle;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "vehicles", indexes = {
    @Index(name = "idx_vehicle_license_plate", columnList = "licensePlate"),
    @Index(name = "idx_vehicle_brand", columnList = "brand_id"),
    @Index(name = "idx_vehicle_model", columnList = "model_id"),
    @Index(name = "idx_vehicle_created_at", columnList = "createdAt")
})
@Data
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String licensePlate;

    private Double weightLimit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private VehicleBrand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id")
    private VehicleModel model;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<VehicleImage> images = new HashSet<>();
}