package nvt.backend.dto.vehicle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistanceStatisticsDTO implements Serializable {
    private Long vehicleId;
    private String licensePlate;
    private LocalDate startDate;
    private LocalDate endDate;
    private String aggregationType;
    private Double totalDistance;
    private List<DistanceDataPoint> dataPoints;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistanceDataPoint implements Serializable {
        private String label;
        private LocalDate startDate;
        private LocalDate endDate;
        private Double distance;
    }
}
