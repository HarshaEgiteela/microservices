
package com.example.productservice.controller;

import com.example.productservice.entity.Product;
import com.example.productservice.service.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // Public: view all products
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(
                productService.getAllProducts()
        );
    }

    // Public: view one product
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                productService.getProductById(id)
        );
    }

    // Admin: create product
    @PostMapping
    public ResponseEntity<Product> createProduct(
            @Valid @RequestBody Product product
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(productService.createProduct(product));
    }

    // Admin: update product
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody Product product
    ) {
        return ResponseEntity.ok(
                productService.updateProduct(id, product)
        );
    }

    // Admin: delete product
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id
    ) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // Used by Order Service during checkout
    @PutMapping("/{id}/reduce-stock")
    public ResponseEntity<Product> reduceStock(
            @PathVariable Long id,
            @RequestParam int quantity
    ) {
        return ResponseEntity.ok(
                productService.reduceQuantity(id, quantity)
        );
    }

    // Used for stock restoration during cancellation
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
