package nvt.backend.dto.vehicle;

import lombok.Data;

@Data
public class CreateVehicleDTO {
    private String licensePlate;
    private Double weightLimit;
    private Long brandId;
    private Long modelId;
}