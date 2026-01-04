package nvt.backend.dto.vehicle;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class VehicleStatusDTO {
    private Long vehicleId;
    private String licensePlate;
    private String brandName;
    private String modelName;
    private boolean online;
    private Double latitude;
    private Double longitude;
    private Double totalDistance;
    private LocalDateTime lastHeartbeat;
    private LocalDateTime lastTelemetry;
    private String currentState;
}
