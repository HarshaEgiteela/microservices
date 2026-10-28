
package com.example.productservice.repository;

import com.example.productservice.entity.Product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository
        extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

    // Reduce stock only if the product is active
    // and sufficient stock is available.
    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query("""
        UPDATE Product p
        SET p.quantity = p.quantity - :quantity
        WHERE p.id = :productId
          AND p.active = true
          AND p.quantity >= :quantity
        """)
    int reduceStockIfAvailable(
            @Param("productId") Long productId,
            @Param("quantity") int quantity
    );

    // Restore stock when an order is cancelled.
    // Inactive products can also have stock restored.
    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query("""
        UPDATE Product p
        SET p.quantity = p.quantity + :quantity
        WHERE p.id = :productId
        """)
    int increaseStock(
            @Param("productId") Long productId,
            @Param("quantity") int quantity
    );
}
