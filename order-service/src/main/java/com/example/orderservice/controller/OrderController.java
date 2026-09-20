package com.example.orderservice.controller;

import com.example.orderservice.dto.Product;
import com.example.orderservice.dto.User;
import com.example.orderservice.entity.OrderEntity;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository repository;
    private final RestTemplate restTemplate;

    @GetMapping
    public List<OrderEntity> getAllOrders() {
        return repository.findAll();
    }

    @PostMapping
    public ResponseEntity<String> placeOrder(
            @RequestParam Long userId,
            @RequestParam Long productId) {

        User user;
        Product product;

        try {
            user = restTemplate.getForObject(
                    "http://user-service:8081/users/" + userId,
                    User.class);
        } catch (HttpClientErrorException.NotFound e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("User not found: " + userId);
        }

        try {
            product = restTemplate.getForObject(
                    "http://product-service:8082/products/" + productId,
                    Product.class);
        } catch (HttpClientErrorException.NotFound e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Product not found: " + productId);
        }

        OrderEntity order =
                new OrderEntity(null, userId, productId);

        repository.save(order);

        return ResponseEntity.ok(
                "Order placed for "
                        + user.getName()
                        + " Product: "
                        + product.getName());
    }
}