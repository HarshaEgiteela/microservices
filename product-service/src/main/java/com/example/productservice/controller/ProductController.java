
package com.example.productservice.controller;

import com.example.productservice.entity.Product;
import com.example.productservice.service.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ========================================
    // PUBLIC: SEARCH, FILTER, SORT, PAGINATE
    // ========================================

    @GetMapping
    public ResponseEntity<Page<Product>> getAllProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "name_asc") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        return ResponseEntity.ok(
                productService.searchProducts(
                        search,
                        category,
                        minPrice,
                        maxPrice,
                        sort,
                        page,
                        size
                )
        );
    }

    // ========================================
    // PUBLIC: VIEW SINGLE PRODUCT
    // ========================================

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                productService.getProductById(id)
        );
    }

    // ========================================
    // ADMIN: CREATE PRODUCT
    // ========================================

    @PostMapping
    public ResponseEntity<Product> createProduct(
            @Valid @RequestBody Product product
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(productService.createProduct(product));
    }

    // ========================================
    // ADMIN: UPDATE PRODUCT
    // ========================================

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody Product product
    ) {
        return ResponseEntity.ok(
                productService.updateProduct(id, product)
        );
    }

    // ========================================
    // ADMIN: SOFT DELETE PRODUCT
    // ========================================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id
    ) {
        productService.deleteProduct(id);

        return ResponseEntity.noContent().build();
    }

    // ========================================
    // CHECKOUT: REDUCE STOCK
    // ========================================

    @PutMapping("/{id}/reduce-stock")
    public ResponseEntity<Product> reduceStock(
            @PathVariable Long id,
            @RequestParam int quantity
    ) {
        return ResponseEntity.ok(
                productService.reduceQuantity(id, quantity)
        );
    }

    // ========================================
    // CANCELLATION: RESTORE STOCK
    // ========================================

    @PutMapping("/{id}/increase-stock")
    public ResponseEntity<Product> increaseStock(
            @PathVariable Long id,
            @RequestParam int quantity
    ) {
        return ResponseEntity.ok(
                productService.increaseQuantity(id, quantity)
        );
    }
}
