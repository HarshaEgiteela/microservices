
package com.example.orderservice.repository;

import com.example.orderservice.entity.OrderEntity;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository
        extends JpaRepository<OrderEntity, Long> {

    // Fetch all orders belonging to a customer
    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(
            Long userId
    );

    // Fetch a specific customer's order
    Optional<OrderEntity> findByIdAndUserId(
            Long orderId,
            Long userId
    );

    // Lock the order during status changes or cancellation
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT o
            FROM OrderEntity o
            WHERE o.id = :orderId
            """)
    Optional<OrderEntity> findByIdForUpdate(
            @Param("orderId") Long orderId
    );
}
