package nvt.backend.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nvt.backend.model.product.Product;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO implements Serializable {
    private Long id;
    private String sku;
    private String name;
    private String description;
    private Long categoryId;
    private String categoryName;
    private BigDecimal price;
    private BigDecimal weight;
    private String unit;
    private boolean active;
    private String imageUrl;
    private Integer totalAvailableQuantity; // Ukupna dostupna količina u svim magacinima

    public static ProductDTO fromEntity(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .price(product.getPrice())
                .weight(product.getWeight())
                .unit(product.getUnit())
                .active(product.isActive())
                .imageUrl(product.getImageUrl())
                .build();
    }

    public static ProductDTO fromEntityWithQuantity(Product product, Integer totalQuantity) {
        ProductDTO dto = fromEntity(product);
        dto.setTotalAvailableQuantity(totalQuantity);
        return dto;
    }
}
