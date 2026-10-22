
package com.example.productservice.repository;

import com.example.productservice.entity.Product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository
        extends JpaRepository<Product, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Product p
        SET p.quantity = p.quantity - :quantity
        WHERE p.id = :productId
          AND p.quantity >= :quantity
        """)
    int reduceStockIfAvailable(
            @Param("productId") Long productId,
            @Param("quantity") int quantity
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
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
