package nvt.backend.repositories.warehouse;

import nvt.backend.model.warehouse.WarehouseInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WarehouseInventoryRepository extends JpaRepository<WarehouseInventory, Long> {

    // Pronađi inventar za određeni magacin i proizvod
    @Query("SELECT wi FROM WarehouseInventory wi " +
           "WHERE wi.warehouse.id = :warehouseId AND wi.product.id = :productId")
    Optional<WarehouseInventory> findByWarehouseAndProduct(
            @Param("warehouseId") Long warehouseId,
            @Param("productId") Long productId);

    // Ukupna dostupna količina proizvoda u svim magacinima
    @Query("SELECT COALESCE(SUM(wi.quantity - wi.reservedQuantity), 0) " +
           "FROM WarehouseInventory wi " +
           "WHERE wi.product.id = :productId")
    Integer getTotalAvailableQuantity(@Param("productId") Long productId);

    // Svi inventari za određeni proizvod (sortirani po dostupnoj količini, od najveće)
    @Query("SELECT wi FROM WarehouseInventory wi " +
           "LEFT JOIN FETCH wi.warehouse " +
           "LEFT JOIN FETCH wi.sector " +
           "WHERE wi.product.id = :productId " +
           "AND (wi.quantity - wi.reservedQuantity) > 0 " +
           "ORDER BY (wi.quantity - wi.reservedQuantity) DESC")
    List<WarehouseInventory> findAvailableInventoryForProduct(@Param("productId") Long productId);

    // Inventar određenog magacina
    @Query("SELECT wi FROM WarehouseInventory wi " +
           "LEFT JOIN FETCH wi.product " +
           "LEFT JOIN FETCH wi.sector " +
           "WHERE wi.warehouse.id = :warehouseId")
    List<WarehouseInventory> findByWarehouseId(@Param("warehouseId") Long warehouseId);

    // Proizvodi sa dostupnom količinom > 0
    @Query("SELECT wi FROM WarehouseInventory wi " +
           "LEFT JOIN FETCH wi.product " +
           "LEFT JOIN FETCH wi.warehouse " +
           "WHERE (wi.quantity - wi.reservedQuantity) > 0")
    List<WarehouseInventory> findAllInStock();

    // Rezerviši količinu (atomična operacija sa optimistic locking)
    @Modifying
    @Query("UPDATE WarehouseInventory wi " +
           "SET wi.reservedQuantity = wi.reservedQuantity + :quantity, " +
           "    wi.lastUpdated = CURRENT_TIMESTAMP " +
           "WHERE wi.id = :inventoryId " +
           "AND (wi.quantity - wi.reservedQuantity) >= :quantity")
    int reserveQuantity(@Param("inventoryId") Long inventoryId, @Param("quantity") Integer quantity);

    // Oslobodi rezervisanu količinu
    @Modifying
    @Query("UPDATE WarehouseInventory wi " +
           "SET wi.reservedQuantity = wi.reservedQuantity - :quantity, " +
           "    wi.lastUpdated = CURRENT_TIMESTAMP " +
           "WHERE wi.id = :inventoryId " +
           "AND wi.reservedQuantity >= :quantity")
    int releaseReservedQuantity(@Param("inventoryId") Long inventoryId, @Param("quantity") Integer quantity);

    // Potvrdi rezervaciju (smanji i quantity i reservedQuantity)
    @Modifying
    @Query("UPDATE WarehouseInventory wi " +
           "SET wi.quantity = wi.quantity - :quantity, " +
           "    wi.reservedQuantity = wi.reservedQuantity - :quantity, " +
           "    wi.lastUpdated = CURRENT_TIMESTAMP " +
           "WHERE wi.id = :inventoryId " +
           "AND wi.reservedQuantity >= :quantity " +
           "AND wi.quantity >= :quantity")
    int confirmReservation(@Param("inventoryId") Long inventoryId, @Param("quantity") Integer quantity);

    // Dodaj količinu (npr. kada fabrika proizvede)
    @Modifying
    @Query("UPDATE WarehouseInventory wi " +
           "SET wi.quantity = wi.quantity + :quantity, " +
           "    wi.lastUpdated = CURRENT_TIMESTAMP " +
           "WHERE wi.id = :inventoryId")
    int addQuantity(@Param("inventoryId") Long inventoryId, @Param("quantity") Integer quantity);

    // Količina po proizvodu grupisana
    @Query("SELECT wi.product.id, SUM(wi.quantity - wi.reservedQuantity) " +
           "FROM WarehouseInventory wi " +
           "WHERE wi.product.id IN :productIds " +
           "GROUP BY wi.product.id")
    List<Object[]> getAvailableQuantitiesForProducts(@Param("productIds") List<Long> productIds);
}
