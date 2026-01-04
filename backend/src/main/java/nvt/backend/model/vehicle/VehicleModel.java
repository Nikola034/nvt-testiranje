package nvt.backend.model.vehicle;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "vehicle_models", indexes = {
    @Index(name = "idx_model_name", columnList = "name"),
    @Index(name = "idx_model_brand", columnList = "brand_id")
})
@Data
public class VehicleModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    @JsonIgnore
    private VehicleBrand brand;
}