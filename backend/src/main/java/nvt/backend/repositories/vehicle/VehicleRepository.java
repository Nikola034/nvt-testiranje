package nvt.backend.repositories.vehicle;

import nvt.backend.model.vehicle.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    @Query("SELECT DISTINCT v FROM Vehicle v " +
            "LEFT JOIN FETCH v.brand " +
            "LEFT JOIN FETCH v.model")
    List<Vehicle> findAllWithDetails();

    @Query("SELECT v.id FROM Vehicle v")
    Page<Long> findAllIds(Pageable pageable);

    @Query("SELECT DISTINCT v FROM Vehicle v " +
            "LEFT JOIN FETCH v.brand " +
            "LEFT JOIN FETCH v.model " +
            "WHERE v.id IN :ids")
    List<Vehicle> findAllByIds(@Param("ids") List<Long> ids);

    @Query("SELECT DISTINCT v FROM Vehicle v " +
            "LEFT JOIN FETCH v.brand " +
            "LEFT JOIN FETCH v.model " +
            "LEFT JOIN FETCH v.images " +
            "WHERE v.id = :id")
    Optional<Vehicle> findByIdWithDetails(@Param("id") Long id);

    @Query(value = "SELECT DISTINCT v.* FROM vehicles v " +
            "LEFT JOIN vehicle_brands b ON b.id = v.brand_id " +
            "LEFT JOIN vehicle_models m ON m.id = v.model_id " +
            "WHERE v.license_plate ILIKE CONCAT('%', :search, '%') " +
            "OR b.name ILIKE CONCAT('%', :search, '%') " +
            "OR m.name ILIKE CONCAT('%', :search, '%')", nativeQuery = true)
    List<Vehicle> searchVehicles(@Param("search") String search);

    @Query(value = "SELECT v.id FROM vehicles v " +
            "LEFT JOIN vehicle_brands b ON b.id = v.brand_id " +
            "LEFT JOIN vehicle_models m ON m.id = v.model_id " +
            "WHERE v.license_plate ILIKE CONCAT('%', :search, '%') " +
            "OR b.name ILIKE CONCAT('%', :search, '%') " +
            "OR m.name ILIKE CONCAT('%', :search, '%')",
            countQuery = "SELECT COUNT(v.id) FROM vehicles v " +
            "LEFT JOIN vehicle_brands b ON b.id = v.brand_id " +
            "LEFT JOIN vehicle_models m ON m.id = v.model_id " +
            "WHERE v.license_plate ILIKE CONCAT('%', :search, '%') " +
            "OR b.name ILIKE CONCAT('%', :search, '%') " +
            "OR m.name ILIKE CONCAT('%', :search, '%')",
            nativeQuery = true)
    Page<Long> searchVehicleIds(@Param("search") String search, Pageable pageable);

    @Query("SELECT v FROM Vehicle v JOIN v.images img WHERE img.id = :imageId")
    Optional<Vehicle> findByImageId(@Param("imageId") Long imageId);

    boolean existsByLicensePlate(String licensePlate);

    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM Vehicle v " +
            "WHERE v.licensePlate = :licensePlate AND v.id != :vehicleId")
    boolean existsByLicensePlateAndIdNot(@Param("licensePlate") String licensePlate, @Param("vehicleId") Long vehicleId);
}