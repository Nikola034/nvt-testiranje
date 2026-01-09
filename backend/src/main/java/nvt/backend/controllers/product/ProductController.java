package nvt.backend.controllers.product;

import lombok.RequiredArgsConstructor;
import nvt.backend.dto.common.PageResponseDTO;
import nvt.backend.dto.product.CategoryDTO;
import nvt.backend.dto.product.ProductDTO;
import nvt.backend.dto.product.ProductSearchDTO;
import nvt.backend.services.product.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * Dohvata sve aktivne proizvode
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllActiveProducts());
    }

    /**
     * Dohvata proizvod po ID-u
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(productService.getProductById(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Pretraga proizvoda sa paginacijom i filterima
     * Efikasan mehanizam pretrage po uzoru na vebšopove
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<PageResponseDTO<ProductDTO>> searchProducts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        ProductSearchDTO searchDTO = new ProductSearchDTO();
        searchDTO.setQuery(query);
        searchDTO.setCategoryId(categoryId);
        searchDTO.setMinPrice(minPrice);
        searchDTO.setMaxPrice(maxPrice);
        searchDTO.setInStock(inStock);
        searchDTO.setSortBy(sortBy);
        searchDTO.setSortDir(sortDir);

        return ResponseEntity.ok(productService.searchProducts(searchDTO, page, size));
    }

    /**
     * Brza pretraga (autocomplete) - vraća max 20 rezultata
     */
    @GetMapping("/quick-search")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<ProductDTO>> quickSearch(@RequestParam String query) {
        return ResponseEntity.ok(productService.quickSearch(query));
    }

    /**
     * Proizvodi koji su na stanju
     */
    @GetMapping("/in-stock")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<ProductDTO>> getProductsInStock() {
        return ResponseEntity.ok(productService.getProductsInStock());
    }

    /**
     * Proizvodi po kategoriji
     */
    @GetMapping("/category/{categoryId}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<ProductDTO>> getProductsByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(productService.getProductsByCategory(categoryId));
    }

    /**
     * Sve kategorije
     */
    @GetMapping("/categories")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
        return ResponseEntity.ok(productService.getAllCategories());
    }

    /**
     * Kategorije sa hijerarhijom (stablo)
     */
    @GetMapping("/categories/tree")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<CategoryDTO>> getCategoryTree() {
        return ResponseEntity.ok(productService.getRootCategories());
    }
}
