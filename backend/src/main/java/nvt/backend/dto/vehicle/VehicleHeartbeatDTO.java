package nvt.backend.dto.vehicle;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class VehicleHeartbeatDTO {
    private Long vehicleId;
    private String licensePlate;
    private String timestamp;
    private String status;
}
