package com.example.orderservice.controller;

import com.example.orderservice.dto.Product;
import com.example.orderservice.dto.User;
import com.example.orderservice.entity.OrderEntity;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository repository;
    private final RestTemplate restTemplate;

    @PostMapping
    public String placeOrder(
            @RequestParam Long userId,
            @RequestParam Long productId) {

        User user = restTemplate.getForObject(
                "http://localhost:8081/users/" + userId,
                User.class);

        Product product = restTemplate.getForObject(
                "http://localhost:8082/products/" + productId,
                Product.class);

        OrderEntity order =
                new OrderEntity(null, userId, productId);

        repository.save(order);

        return "Order placed for "
                + user.getName()
                + " Product: "
                + product.getName();
    }
}