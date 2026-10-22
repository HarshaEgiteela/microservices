
package com.example.orderservice.repository;

import com.example.orderservice.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository
        extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByIdAndCartUserId(
            Long itemId,
            Long userId
    );

    Optional<CartItem> findByCartIdAndProductId(
            Long cartId,
            Long productId
    );
}
