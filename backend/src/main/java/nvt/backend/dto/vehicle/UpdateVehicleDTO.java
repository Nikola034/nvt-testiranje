package nvt.backend.dto.vehicle;

import lombok.Data;

import java.util.List;

@Data
public class UpdateVehicleDTO {
    private String licensePlate;
    private Double weightLimit;
    private Long brandId;
    private Long modelId;
    private List<Long> imagesToDelete;
}