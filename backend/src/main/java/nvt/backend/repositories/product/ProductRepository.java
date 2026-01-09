package nvt.backend.repositories.product;

import nvt.backend.model.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.category " +
           "WHERE p.active = true")
    List<Product> findAllActive();

    @Query("SELECT p FROM Product p " +
           "LEFT JOIN FETCH p.category " +
           "WHERE p.id = :id")
    Optional<Product> findByIdWithCategory(@Param("id") Long id);

    // Full-text search - efikasna pretraga po imenu, SKU i opisu
    @Query(value = "SELECT DISTINCT p.* FROM products p " +
           "LEFT JOIN product_categories c ON c.id = p.category_id " +
           "WHERE p.active = true " +
           "AND (p.name ILIKE CONCAT('%', :query, '%') " +
           "     OR p.sku ILIKE CONCAT('%', :query, '%') " +
           "     OR p.description ILIKE CONCAT('%', :query, '%'))",
           nativeQuery = true)
    List<Product> searchProducts(@Param("query") String query);

    // Paginirana pretraga
    @Query(value = "SELECT p.id FROM products p " +
           "LEFT JOIN product_categories c ON c.id = p.category_id " +
           "WHERE p.active = true " +
           "AND (:query IS NULL OR :query = '' OR " +
           "     p.name ILIKE CONCAT('%', :query, '%') OR " +
           "     p.sku ILIKE CONCAT('%', :query, '%') OR " +
           "     p.description ILIKE CONCAT('%', :query, '%')) " +
           "AND (:categoryId IS NULL OR p.category_id = :categoryId) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice)",
           countQuery = "SELECT COUNT(p.id) FROM products p " +
           "WHERE p.active = true " +
           "AND (:query IS NULL OR :query = '' OR " +
           "     p.name ILIKE CONCAT('%', :query, '%') OR " +
           "     p.sku ILIKE CONCAT('%', :query, '%') OR " +
           "     p.description ILIKE CONCAT('%', :query, '%')) " +
           "AND (:categoryId IS NULL OR p.category_id = :categoryId) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice)",
           nativeQuery = true)
    Page<Long> searchProductIds(
            @Param("query") String query,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);

    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.category " +
           "WHERE p.id IN :ids")
    List<Product> findAllByIds(@Param("ids") List<Long> ids);

    // Proizvodi po kategoriji
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.active = true")
    List<Product> findByCategoryId(@Param("categoryId") Long categoryId);

    // Proizvodi koji su na stanju (imaju zalihe u bilo kom magacinu)
    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.category " +
           "WHERE p.active = true " +
           "AND EXISTS (SELECT 1 FROM WarehouseInventory wi " +
           "            WHERE wi.product = p AND (wi.quantity - wi.reservedQuantity) > 0)")
    List<Product> findProductsInStock();

    @Query(value = "SELECT p.id FROM products p " +
           "WHERE p.active = true " +
           "AND EXISTS (SELECT 1 FROM warehouse_inventory wi " +
           "            WHERE wi.product_id = p.id AND (wi.quantity - wi.reserved_quantity) > 0) " +
           "AND (:query IS NULL OR :query = '' OR " +
           "     p.name ILIKE CONCAT('%', :query, '%') OR " +
           "     p.sku ILIKE CONCAT('%', :query, '%'))",
           nativeQuery = true)
    Page<Long> searchInStockProductIds(@Param("query") String query, Pageable pageable);
}
