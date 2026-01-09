package nvt.backend.repositories.order;

import nvt.backend.model.order.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("SELECT oi FROM OrderItem oi " +
           "LEFT JOIN FETCH oi.product " +
           "LEFT JOIN FETCH oi.warehouse " +
           "WHERE oi.order.id = :orderId")
    List<OrderItem> findByOrderIdWithDetails(@Param("orderId") Long orderId);
}
