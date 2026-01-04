package nvt.backend.dto.vehicle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nvt.backend.model.vehicle.Vehicle;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleListDTO implements Serializable {
    private Long id;
    private String licensePlate;
    private Double weightLimit;
    private Long brandId;
    private String brandName;
    private Long modelId;
    private String modelName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int imageCount;

    public static VehicleListDTO fromEntity(Vehicle vehicle) {
        return VehicleListDTO.builder()
                .id(vehicle.getId())
                .licensePlate(vehicle.getLicensePlate())
                .weightLimit(vehicle.getWeightLimit())
                .brandId(vehicle.getBrand().getId())
                .brandName(vehicle.getBrand().getName())
                .modelId(vehicle.getModel().getId())
                .modelName(vehicle.getModel().getName())
                .createdAt(vehicle.getCreatedAt())
                .updatedAt(vehicle.getUpdatedAt())
                .imageCount(0)
                .build();
    }
}
