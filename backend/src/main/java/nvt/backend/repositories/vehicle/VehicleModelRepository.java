package nvt.backend.repositories.vehicle;

import nvt.backend.model.vehicle.VehicleModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleModelRepository extends JpaRepository<VehicleModel, Long> {
    List<VehicleModel> findByBrandId(Long brandId);
}