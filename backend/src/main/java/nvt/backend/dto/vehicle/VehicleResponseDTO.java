package nvt.backend.dto.vehicle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nvt.backend.model.vehicle.Vehicle;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleResponseDTO implements Serializable {
    private Long id;
    private String licensePlate;
    private Double weightLimit;
    private Long brandId;
    private String brandName;
    private Long modelId;
    private String modelName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ImageDTO> images;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageDTO implements Serializable {
        private Long id;
        private String originalName;
    }

    public static VehicleResponseDTO fromEntity(Vehicle vehicle) {
        return VehicleResponseDTO.builder()
                .id(vehicle.getId())
                .licensePlate(vehicle.getLicensePlate())
                .weightLimit(vehicle.getWeightLimit())
                .brandId(vehicle.getBrand().getId())
                .brandName(vehicle.getBrand().getName())
                .modelId(vehicle.getModel().getId())
                .modelName(vehicle.getModel().getName())
                .createdAt(vehicle.getCreatedAt())
                .updatedAt(vehicle.getUpdatedAt())
                .images(vehicle.getImages().stream()
                        .map(img -> ImageDTO.builder()
                                .id(img.getId())
                                .originalName(img.getOriginalName())
                                .build())
                        .toList())
                .build();
    }
}