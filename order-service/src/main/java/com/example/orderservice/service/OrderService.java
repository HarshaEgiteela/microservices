package com.example.orderservice.service;

import com.example.orderservice.dto.Product;
import com.example.orderservice.dto.User;
import com.example.orderservice.entity.OrderEntity;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.repository.OrderRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;
    private final HttpServletRequest httpRequest;

    private HttpEntity<Void> createAuthenticatedRequest() {

        String authorization =
                httpRequest.getHeader("Authorization");

        HttpHeaders headers = new HttpHeaders();

        if (authorization != null) {
            headers.set("Authorization", authorization);
        }

        return new HttpEntity<>(headers);
    }

    public String placeOrder(
            Long userId,
            Long productId,
            int quantity) {

        if (quantity <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Quantity must be greater than zero"
            );
        }

        // Check user
        User user;

        try {

            ResponseEntity<User> userResponse =
                    restTemplate.exchange(
                            "http://user-service/users/" + userId,
                            HttpMethod.GET,
                            createAuthenticatedRequest(),
                            User.class
                    );

            user = userResponse.getBody();

        } catch (HttpClientErrorException.NotFound e) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found: " + userId
            );
        }

        // Check product
        Product product;

        try {

            ResponseEntity<Product> productResponse =
                    restTemplate.exchange(
                            "http://product-service/products/" + productId,
                            HttpMethod.GET,
                            createAuthenticatedRequest(),
                            Product.class
                    );

            product = productResponse.getBody();

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

            restTemplate.exchange(
                    "http://product-service/products/"
                            + productId
                            + "/reduce-stock?quantity="
                            + quantity,
                    HttpMethod.PUT,
                    createAuthenticatedRequest(),
                    Void.class
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
                quantity,
                OrderStatus.PLACED
        );

        orderRepository.save(order);

        return "Order placed for "
                + user.getName()
                + " | Product: "
                + product.getName()
                + " | Quantity: "
                + quantity;
    }

    public OrderEntity updateOrderStatus(
            Long orderId,
            OrderStatus newStatus) {

        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Order not found: " + orderId
                        )
                );

        OrderStatus currentStatus = order.getStatus();

        boolean validTransition =
                switch (currentStatus) {

                    case PLACED ->
                            newStatus == OrderStatus.CONFIRMED ||
                                    newStatus == OrderStatus.CANCELLED;

                    case CONFIRMED ->
                            newStatus == OrderStatus.PROCESSING ||
                                    newStatus == OrderStatus.CANCELLED;

                    case PROCESSING ->
                            newStatus == OrderStatus.SHIPPED ||
                                    newStatus == OrderStatus.CANCELLED;

                    case SHIPPED ->
                            newStatus == OrderStatus.DELIVERED;

                    case DELIVERED, CANCELLED ->
                            false;
                };

        if (!validTransition) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid status transition: "
                            + currentStatus
                            + " → "
                            + newStatus
            );
        }

        order.setStatus(newStatus);

        return orderRepository.save(order);
    }
}