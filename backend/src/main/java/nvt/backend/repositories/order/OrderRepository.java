package nvt.backend.repositories.order;

import nvt.backend.model.order.Order;
import nvt.backend.model.order.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.customer " +
           "LEFT JOIN FETCH o.deliveryCompany " +
           "LEFT JOIN FETCH o.items i " +
           "LEFT JOIN FETCH i.product " +
           "WHERE o.id = :id")
    Optional<Order> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.customer " +
           "LEFT JOIN FETCH o.deliveryCompany " +
           "WHERE o.customer.id = :customerId " +
           "ORDER BY o.createdAt DESC")
    List<Order> findByCustomerId(@Param("customerId") Integer customerId);

    @Query("SELECT o.id FROM Order o WHERE o.customer.id = :customerId")
    Page<Long> findIdsByCustomerId(@Param("customerId") Integer customerId, Pageable pageable);

    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.customer " +
           "LEFT JOIN FETCH o.deliveryCompany " +
           "LEFT JOIN FETCH o.items i " +
           "LEFT JOIN FETCH i.product " +
           "WHERE o.id IN :ids")
    List<Order> findAllByIdsWithDetails(@Param("ids") List<Long> ids);

    @Query("SELECT o FROM Order o WHERE o.status = :status")
    List<Order> findByStatus(@Param("status") OrderStatus status);

    // Poslednji order number za generisanje sledećeg
    @Query("SELECT o.orderNumber FROM Order o ORDER BY o.id DESC LIMIT 1")
    Optional<String> findLastOrderNumber();

    // Statistika za menadžere
    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countByStatus();

    // Sve narudžbine za menadžere
    @Query("SELECT o.id FROM Order o")
    Page<Long> findAllIds(Pageable pageable);
}
