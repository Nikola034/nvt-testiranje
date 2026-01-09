package nvt.backend.services.product;

import lombok.RequiredArgsConstructor;
import nvt.backend.dto.common.PageResponseDTO;
import nvt.backend.dto.product.CategoryDTO;
import nvt.backend.dto.product.ProductDTO;
import nvt.backend.dto.product.ProductSearchDTO;
import nvt.backend.model.product.Product;
import nvt.backend.model.product.ProductCategory;
import nvt.backend.repositories.product.ProductCategoryRepository;
import nvt.backend.repositories.product.ProductRepository;
import nvt.backend.repositories.warehouse.WarehouseInventoryRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final WarehouseInventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public List<ProductDTO> getAllActiveProducts() {
        List<Product> products = productRepository.findAllActive();
        return enrichWithQuantities(products);
    }

    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        Integer quantity = inventoryRepository.getTotalAvailableQuantity(id);
        return ProductDTO.fromEntityWithQuantity(product, quantity);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "productSearch", key = "#searchDTO.toString() + '-' + #page + '-' + #size")
    public PageResponseDTO<ProductDTO> searchProducts(ProductSearchDTO searchDTO, int page, int size) {
        Sort sort = buildSort(searchDTO.getSortBy(), searchDTO.getSortDir());
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Long> idPage;
        
        if (Boolean.TRUE.equals(searchDTO.getInStock())) {
            // Pretraga samo proizvoda na stanju
            idPage = productRepository.searchInStockProductIds(searchDTO.getQuery(), pageable);
        } else {
            // Opšta pretraga sa filterima
            idPage = productRepository.searchProductIds(
                    searchDTO.getQuery(),
                    searchDTO.getCategoryId(),
                    searchDTO.getMinPrice(),
                    searchDTO.getMaxPrice(),
                    pageable
            );
        }

        List<Product> products = idPage.getContent().isEmpty()
                ? List.of()
                : productRepository.findAllByIds(idPage.getContent());

        List<ProductDTO> dtos = enrichWithQuantities(products);

        // Sortiraj po redosledu ID-jeva iz originalne pretrage
        Map<Long, Integer> orderMap = new HashMap<>();
        List<Long> ids = idPage.getContent();
        for (int i = 0; i < ids.size(); i++) {
            orderMap.put(ids.get(i), i);
        }
        dtos.sort((a, b) -> orderMap.getOrDefault(a.getId(), 0) - orderMap.getOrDefault(b.getId(), 0));

        return PageResponseDTO.<ProductDTO>builder()
                .content(dtos)
                .page(idPage.getNumber())
                .size(idPage.getSize())
                .totalElements(idPage.getTotalElements())
                .totalPages(idPage.getTotalPages())
                .first(idPage.isFirst())
                .last(idPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> quickSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        List<Product> products = productRepository.searchProducts(query.trim());
        return enrichWithQuantities(products.stream().limit(20).collect(Collectors.toList()));
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsInStock() {
        List<Product> products = productRepository.findProductsInStock();
        return enrichWithQuantities(products);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "categories")
    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(CategoryDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "rootCategories")
    public List<CategoryDTO> getRootCategories() {
        return categoryRepository.findRootCategories().stream()
                .map(this::mapCategoryWithChildren)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByCategory(Long categoryId) {
        List<Product> products = productRepository.findByCategoryId(categoryId);
        return enrichWithQuantities(products);
    }

    // Pomoćne metode
    
    private List<ProductDTO> enrichWithQuantities(List<Product> products) {
        if (products.isEmpty()) {
            return List.of();
        }

        List<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
        List<Object[]> quantities = inventoryRepository.getAvailableQuantitiesForProducts(productIds);
        
        Map<Long, Integer> quantityMap = new HashMap<>();
        for (Object[] row : quantities) {
            Long productId = (Long) row[0];
            Long qty = (Long) row[1];
            quantityMap.put(productId, qty != null ? qty.intValue() : 0);
        }

        return products.stream()
                .map(p -> ProductDTO.fromEntityWithQuantity(p, quantityMap.getOrDefault(p.getId(), 0)))
                .collect(Collectors.toList());
    }

    private Sort buildSort(String sortBy, String sortDir) {
        String field = switch (sortBy.toLowerCase()) {
            case "price" -> "price";
            case "sku" -> "sku";
            default -> "name";
        };
        
        return sortDir.equalsIgnoreCase("desc") 
                ? Sort.by(field).descending() 
                : Sort.by(field).ascending();
    }

    private CategoryDTO mapCategoryWithChildren(ProductCategory category) {
        CategoryDTO dto = CategoryDTO.fromEntity(category);
        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            dto.setChildren(category.getChildren().stream()
                    .map(this::mapCategoryWithChildren)
                    .collect(Collectors.toList()));
        }
        return dto;
    }
}
