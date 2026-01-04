package nvt.backend.repositories.vehicle;

import nvt.backend.model.vehicle.VehicleBrand;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleBrandRepository extends JpaRepository<VehicleBrand, Long> {
}