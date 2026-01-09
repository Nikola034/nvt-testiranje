package nvt.backend.repositories.product;

import nvt.backend.model.product.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    @Query("SELECT c FROM ProductCategory c WHERE c.parent IS NULL")
    List<ProductCategory> findRootCategories();

    @Query("SELECT c FROM ProductCategory c WHERE c.parent.id = :parentId")
    List<ProductCategory> findByParentId(Long parentId);
}
