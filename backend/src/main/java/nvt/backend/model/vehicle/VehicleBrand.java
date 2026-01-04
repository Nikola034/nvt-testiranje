package nvt.backend.model.vehicle;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "vehicle_brands", indexes = {
    @Index(name = "idx_brand_name", columnList = "name")
})
@Data
public class VehicleBrand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "brand")
    @JsonIgnore
    private List<VehicleModel> models;
}