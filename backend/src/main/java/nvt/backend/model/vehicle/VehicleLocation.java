package nvt.backend.model.vehicle;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_locations", indexes = {
    @Index(name = "idx_location_vehicle", columnList = "vehicle_id"),
    @Index(name = "idx_location_online", columnList = "online"),
    @Index(name = "idx_location_last_heartbeat", columnList = "lastHeartbeat")
})
@Data
public class VehicleLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", unique = true)
    private Vehicle vehicle;

    private Double latitude;
    private Double longitude;
    private Double totalDistance;
    private LocalDateTime lastHeartbeat;
    private LocalDateTime lastTelemetryUpdate;
    private boolean online;
    private String currentState;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
