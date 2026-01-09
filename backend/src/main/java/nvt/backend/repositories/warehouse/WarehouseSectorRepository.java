package nvt.backend.repositories.warehouse;

import nvt.backend.model.warehouse.WarehouseSector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WarehouseSectorRepository extends JpaRepository<WarehouseSector, Long> {

    @Query("SELECT s FROM WarehouseSector s WHERE s.warehouse.id = :warehouseId AND s.active = true")
    List<WarehouseSector> findByWarehouseId(@Param("warehouseId") Long warehouseId);

    @Query("SELECT s FROM WarehouseSector s WHERE s.warehouse.code = :warehouseCode AND s.active = true")
    List<WarehouseSector> findByWarehouseCode(@Param("warehouseCode") String warehouseCode);
}
