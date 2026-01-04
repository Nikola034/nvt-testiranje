package nvt.backend.dto.vehicle;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class VehicleStateDTO {
    private Long vehicleId;
    private String licensePlate;
    private String timestamp;
    private String state;
}
