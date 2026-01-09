package nvt.backend.repositories.warehouse;

import nvt.backend.model.warehouse.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    Optional<Warehouse> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT DISTINCT w FROM Warehouse w " +
           "LEFT JOIN FETCH w.country " +
           "LEFT JOIN FETCH w.city " +
           "WHERE w.active = true")
    List<Warehouse> findAllActiveWithDetails();

    @Query("SELECT DISTINCT w FROM Warehouse w " +
           "LEFT JOIN FETCH w.country " +
           "LEFT JOIN FETCH w.city " +
           "LEFT JOIN FETCH w.sectors " +
           "WHERE w.id = :id")
    Optional<Warehouse> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT w FROM Warehouse w WHERE w.online = true")
    List<Warehouse> findAllOnline();
}
