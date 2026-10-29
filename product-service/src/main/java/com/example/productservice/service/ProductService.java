
package com.example.productservice.service;

import com.example.productservice.entity.Product;
import com.example.productservice.exception.ResourceNotFoundException;
import com.example.productservice.repository.ProductRepository;
import com.example.productservice.specification.ProductSpecification;

import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductPageCacheService productPageCacheService;

    // ========================================
    // PUBLIC: SEARCH, FILTER, SORT, PAGINATE
    // ========================================

    // Search and pagination use MySQL on cache misses; Redis caches a DTO.
    @Transactional(readOnly = true)
    public Page<Product> searchProducts(
            String search,
            String category,
            Double minPrice,
            Double maxPrice,
            String sort,
            int page,
            int size
    ) {

        // Validate pagination
        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative"
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100"
            );
        }

        // Validate price range
        if (minPrice != null &&
                (!Double.isFinite(minPrice) || minPrice < 0)) {
            throw new IllegalArgumentException(
                    "Minimum price must be a valid non-negative number"
            );
        }

        if (maxPrice != null &&
                (!Double.isFinite(maxPrice) || maxPrice < 0)) {
            throw new IllegalArgumentException(
                    "Maximum price must be a valid non-negative number"
            );
        }

        if (minPrice != null && maxPrice != null
                && minPrice > maxPrice) {
            throw new IllegalArgumentException(
                    "Minimum price cannot exceed maximum price"
            );
        }

        // Sorting options
        String sortOption = sort == null
                ? "name_asc"
                : sort;

        Sort sorting = switch (sortOption) {

            case "price_asc" ->
                    Sort.by("price").ascending()
                            .and(Sort.by("id").ascending());

            case "price_desc" ->
                    Sort.by("price").descending()
                            .and(Sort.by("id").ascending());

            case "name_desc" ->
                    Sort.by("name").descending()
                            .and(Sort.by("id").ascending());

            case "name_asc" ->
                    Sort.by("name").ascending()
                            .and(Sort.by("id").ascending());

            default ->
                    throw new IllegalArgumentException(
                            "Invalid sort option: " + sortOption
                    );
        };

        // Zero-based pagination:
        // page=0 -> first page
        // page=1 -> second page
        PageRequest pageable = PageRequest.of(
                page,
                size,
                sorting
        );

        CachedProductPage cached = productPageCacheService.getPage(
                search, category, minPrice, maxPrice, sortOption, page, size
        );
        return new PageImpl<>(cached.content(), pageable, cached.totalElements());
    }

    // ========================================
    // GET PRODUCT BY ID
    // ========================================

    // Read directly from MySQL.
    @Transactional(readOnly = true)
    public Product getProductById(Long id) {

        return productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found: " + id
                        )
                );
    }

    // ========================================
    // ADMIN: CREATE PRODUCT
    // ========================================

    @CacheEvict(value = "activeProducts", allEntries = true)
    @Transactional
    public Product createProduct(Product product) {

        validateProduct(product);

        product.setId(null);
        product.setActive(true);

        return productRepository.save(product);
    }

    // ========================================
    // ADMIN: UPDATE PRODUCT
    // ========================================

    @CacheEvict(value = "activeProducts", allEntries = true)
    @Transactional
    public Product updateProduct(
            Long id,
            Product updatedProduct
    ) {

        validateProduct(updatedProduct);

        Product product = getProductById(id);

        product.setName(updatedProduct.getName());
        product.setPrice(updatedProduct.getPrice());
        product.setQuantity(updatedProduct.getQuantity());
        product.setDescription(updatedProduct.getDescription());
        product.setCategory(updatedProduct.getCategory());
        product.setImageUrl(updatedProduct.getImageUrl());

        return productRepository.save(product);
    }

    // ========================================
    // ADMIN: SOFT DELETE PRODUCT
    // ========================================

    @CacheEvict(value = "activeProducts", allEntries = true)
    @Transactional
    public void deleteProduct(Long id) {

        Product product = getProductById(id);

        product.setActive(false);

        productRepository.save(product);
    }

    // ========================================
    // CHECKOUT: ATOMIC STOCK REDUCTION
    // ========================================

    @CacheEvict(value = "activeProducts", allEntries = true)
    @Transactional
    public Product reduceQuantity(
            Long productId,
            int quantity
    ) {

        validateStockQuantity(quantity);

        Product product = getProductById(productId);

        if (!product.isActive()) {
            throw new IllegalArgumentException(
                    "Product is no longer available: " + productId
            );
        }

        int updatedRows =
                productRepository.reduceStockIfAvailable(
                        productId,
                        quantity
                );

        if (updatedRows == 0) {

            if (!productRepository.existsById(productId)) {
                throw new ResourceNotFoundException(
                        "Product not found: " + productId
                );
            }

            throw new IllegalArgumentException(
                    "Insufficient stock for product: " + productId
            );
        }

        return getProductById(productId);
    }

    // ========================================
    // CANCELLATION: STOCK RESTORATION
    // ========================================

    @CacheEvict(value = "activeProducts", allEntries = true)
    @Transactional
    public Product increaseQuantity(
            Long productId,
            int quantity
    ) {

        validateStockQuantity(quantity);

        int updatedRows =
                productRepository.increaseStock(
                        productId,
                        quantity
                );

        if (updatedRows == 0) {
            throw new ResourceNotFoundException(
                    "Product not found: " + productId
            );
        }

        return getProductById(productId);
    }

    // ========================================
    // VALIDATION HELPERS
    // ========================================

    private void validateStockQuantity(int quantity) {

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero"
            );
        }
    }

    private void validateProduct(Product product) {

        if (product == null) {
            throw new IllegalArgumentException(
                    "Product is required"
            );
        }

        if (product.getName() == null
                || product.getName().isBlank()) {
            throw new IllegalArgumentException(
                    "Product name is required"
            );
        }

        if (!Double.isFinite(product.getPrice())
                || product.getPrice() < 0) {
            throw new IllegalArgumentException(
                    "Price must be a valid non-negative number"
            );
        }

        if (product.getQuantity() < 0) {
            throw new IllegalArgumentException(
                    "Quantity cannot be negative"
            );
        }

        product.setName(product.getName().trim());

        // Category remains optional until existing
        // database products have been updated.
        if (product.getCategory() != null) {

            String category = product.getCategory().trim();

            if (category.length() > 100) {
                throw new IllegalArgumentException(
                        "Category cannot exceed 100 characters"
                );
            }

            product.setCategory(
                    category.isEmpty() ? null : category
            );
        }

        // Optional product image URL
        if (product.getImageUrl() != null) {

            String imageUrl = product.getImageUrl().trim();

            if (imageUrl.length() > 2000) {
                throw new IllegalArgumentException(
                        "Image URL cannot exceed 2000 characters"
                );
            }

            product.setImageUrl(
                    imageUrl.isEmpty() ? null : imageUrl
            );
        }
    }
}
