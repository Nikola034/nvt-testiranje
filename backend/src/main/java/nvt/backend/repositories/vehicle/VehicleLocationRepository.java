package nvt.backend.repositories.vehicle;

import nvt.backend.model.vehicle.VehicleLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VehicleLocationRepository extends JpaRepository<VehicleLocation, Long> {

    Optional<VehicleLocation> findByVehicleId(Long vehicleId);

    @Query("SELECT vl FROM VehicleLocation vl WHERE vl.online = true")
    List<VehicleLocation> findAllOnline();

    @Query("SELECT vl FROM VehicleLocation vl WHERE vl.online = true AND vl.lastHeartbeat < :threshold")
    List<VehicleLocation> findStaleOnlineVehicles(@Param("threshold") LocalDateTime threshold);

    @Modifying
    @Query("UPDATE VehicleLocation vl SET vl.online = false WHERE vl.online = true AND vl.lastHeartbeat < :threshold")
    int markOfflineByHeartbeatThreshold(@Param("threshold") LocalDateTime threshold);
}
