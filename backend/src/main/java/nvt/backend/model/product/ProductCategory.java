package nvt.backend.model.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "product_categories", indexes = {
    @Index(name = "idx_category_name", columnList = "name")
})
@Data
public class ProductCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonIgnore
    private ProductCategory parent;

    @OneToMany(mappedBy = "parent")
    @JsonIgnore
    private List<ProductCategory> children;

    @OneToMany(mappedBy = "category")
    @JsonIgnore
    private List<Product> products;
}
