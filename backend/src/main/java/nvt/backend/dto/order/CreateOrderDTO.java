package nvt.backend.dto.order;

import lombok.Data;

import java.util.List;

@Data
public class CreateOrderDTO {
    private Long deliveryCompanyId;
    private List<OrderItemDTO> items;
    private String notes;

    @Data
    public static class OrderItemDTO {
        private Long productId;
        private Integer quantity;
    }
}
