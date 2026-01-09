package nvt.backend.dto.product;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSearchDTO {
    private String query; // Pretraga po imenu, SKU, opisu
    private Long categoryId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Boolean inStock; // Samo proizvodi koji su na stanju
    private String sortBy = "name"; // name, price, sku
    private String sortDir = "asc";
}
