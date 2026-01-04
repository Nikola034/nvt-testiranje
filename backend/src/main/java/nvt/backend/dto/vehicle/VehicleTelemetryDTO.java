package nvt.backend.dto.vehicle;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class VehicleTelemetryDTO {
    private Long vehicleId;
    private String licensePlate;
    private String timestamp;
    private Double latitude;
    private Double longitude;
    private Double distanceTraveled;
}