
package com.example.productservice.service;

import com.example.productservice.entity.Product;
import com.example.productservice.exception.ResourceNotFoundException;
import com.example.productservice.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    // ========================================
    // PUBLIC: GET ALL ACTIVE PRODUCTS
    // ========================================

    // First request: fetch from MySQL and cache in Redis.
    // Later requests: return cached data until eviction or TTL.
    @Cacheable(value = "activeProducts", key = "'all'")
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {

        return productRepository.findAll()
                .stream()
                .filter(Product::isActive)
                .toList();
    }

    // ========================================
    // GET PRODUCT BY ID
    // ========================================

    // Not cached: stock operations must read from MySQL.
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

    // Clear cached product list after successful commit.
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

        product.setName(updatedProduct.getName().trim());
        product.setPrice(updatedProduct.getPrice());
        product.setQuantity(updatedProduct.getQuantity());
        product.setDescription(updatedProduct.getDescription());

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
    // CHECKOUT: REDUCE STOCK
    // ========================================

    // Stock is always updated through MySQL.
    // Redis is used only for product-list caching.
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
    // RESTORE STOCK
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
    }
}
