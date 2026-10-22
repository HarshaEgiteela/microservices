
package com.example.orderservice.controller;

import com.example.orderservice.dto.CheckoutRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(
            Authentication authentication,
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody CheckoutRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();

        if (!authorization.startsWith("Bearer ")
                || authorization.length() <= 7) {
            throw new IllegalArgumentException("Valid Bearer token is required");
        }

        String token = authorization.substring(7);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(orderService.checkout(
                        userId,
                        request.addressId(),
                        token
                ));
    }

    @GetMapping("/my")
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return ResponseEntity.ok(
                orderService.getMyOrders(userId)
        );
    }

    @GetMapping("/my/{orderId}")
    public ResponseEntity<OrderResponse> getMyOrder(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return ResponseEntity.ok(
                orderService.getMyOrder(userId, orderId)
        );
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(
                orderService.getAllOrders()
        );
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status
    ) {
        return ResponseEntity.ok(
                orderService.updateOrderStatus(orderId, status)
        );
    }

    @GetMapping("/instance")
    public String getInstance() throws UnknownHostException {
        return "Order Service instance: "
                + InetAddress.getLocalHost().getHostName();
    }
}
