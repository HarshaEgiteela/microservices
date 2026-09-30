package com.example.orderservice.service;

import com.example.orderservice.dto.Product;
import com.example.orderservice.dto.User;
import com.example.orderservice.entity.OrderEntity;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;

    public String placeOrder(
            Long userId,
            Long productId,
            int quantity) {

        // Validate quantity
        if (quantity <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Quantity must be greater than zero"
            );
        }

        // Check user
        User user;

        try {
            user = restTemplate.getForObject(
                    "http://user-service/users/" + userId,
                    User.class
            );

        } catch (HttpClientErrorException.NotFound e) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found: " + userId
            );
        }

        // Check product
        Product product;

        try {
            product = restTemplate.getForObject(
                    "http://product-service/products/" + productId,
                    Product.class
            );

        } catch (HttpClientErrorException.NotFound e) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Product not found: " + productId
            );
        }

        // Check stock
        if (product.getQuantity() < quantity) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Insufficient stock. Available: "
                            + product.getQuantity()
            );
        }

        // Reduce product quantity
        try {

            restTemplate.put(
                    "http://product-service/products/"
                            + productId
                            + "/reduce-stock?quantity="
                            + quantity,
                    null
            );

        } catch (HttpClientErrorException e) {

            throw new ResponseStatusException(
                    e.getStatusCode(),
                    e.getResponseBodyAsString()
            );
        }

        // Create order
        OrderEntity order = new OrderEntity(
                null,
                userId,
                productId,
                quantity
        );

        orderRepository.save(order);

        return "Order placed for "
                + user.getName()
                + " | Product: "
                + product.getName()
                + " | Quantity: "
                + quantity;
    }
}