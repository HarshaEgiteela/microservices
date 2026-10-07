package com.example.orderservice.controller;

import com.example.orderservice.entity.OrderEntity;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository repository;
    private final OrderService orderService;


    @GetMapping
    public List<OrderEntity> getAllOrders() {
        return repository.findAll();
    }


    @GetMapping("/instance")
    public String getInstance() throws UnknownHostException {

        return "Order Service instance: "
                + InetAddress.getLocalHost().getHostName();
    }


    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderEntity> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status) {

        return ResponseEntity.ok(
                orderService.updateOrderStatus(
                        orderId,
                        status
                )
        );
    }


    @PostMapping
    public ResponseEntity<String> placeOrder(
            @RequestParam Long userId,
            @RequestParam Long productId,
            @RequestParam int quantity) {

        return ResponseEntity.ok(
                orderService.placeOrder(
                        userId,
                        productId,
                        quantity
                )
        );
    }
}