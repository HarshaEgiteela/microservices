
package com.example.orderservice.service;

import com.example.orderservice.client.ProductClient;
import com.example.orderservice.client.UserClient;
import com.example.orderservice.dto.AddressResponse;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.dto.Product;
import com.example.orderservice.entity.*;
import com.example.orderservice.exception.ResourceNotFoundException;
import com.example.orderservice.repository.CartRepository;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductClient productClient;
    private final UserClient userClient;

    @Transactional
    public OrderResponse checkout(
            Long userId,
            Long addressId,
            String token
    ) {

        Cart cart = cartRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Cart is empty"
                        )
                );

        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException(
                    "Cart is empty"
            );
        }

        AddressResponse address =
                userClient.getAddress(addressId, token);

        OrderEntity order = new OrderEntity();
        order.setUserId(userId);
        order.setStatus(OrderStatus.PLACED);
        order.setAddressId(address.id());
        order.setRecipientName(address.recipientName());
        order.setPhone(address.phone());
        order.setAddressLine1(address.addressLine1());
        order.setAddressLine2(address.addressLine2());
        order.setCity(address.city());
        order.setState(address.state());
        order.setPostalCode(address.postalCode());
        order.setCountry(address.country());

        BigDecimal total = BigDecimal.ZERO;

        List<CartItem> cartItems =
                new ArrayList<>(cart.getItems());

        for (CartItem cartItem : cartItems) {

            Product product =
                    productClient.getProduct(
                            cartItem.getProductId(),
                            token
                    );

            if (cartItem.getQuantity() >
                    product.getQuantity()) {
                throw new IllegalArgumentException(
                        "Insufficient stock for product: "
                                + product.getName()
                );
            }

            BigDecimal price =
                    BigDecimal.valueOf(product.getPrice());

            BigDecimal subtotal = price.multiply(
                    BigDecimal.valueOf(cartItem.getQuantity())
            );

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(price);
            orderItem.setSubtotal(subtotal);

            order.addItem(orderItem);

            total = total.add(subtotal);
        }

        order.setTotalAmount(total);

        // Stock changes happen in another microservice.
        // These calls cannot be rolled back by this transaction.
        for (CartItem cartItem : cartItems) {
            productClient.reduceStock(
                    cartItem.getProductId(),
                    cartItem.getQuantity(),
                    token
            );
        }

        OrderEntity savedOrder = orderRepository.save(order);

        cart.getItems().clear();

        return toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(Long userId) {

        return orderRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(
            Long userId,
            Long orderId
    ) {

        OrderEntity order = orderRepository
                .findByIdAndUserId(orderId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found: " + orderId
                        )
                );

        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {

        return orderRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OrderResponse updateOrderStatus(
            Long orderId,
            OrderStatus newStatus
    ) {

        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found: " + orderId
                        )
                );

        OrderStatus currentStatus = order.getStatus();

        boolean validTransition = switch (currentStatus) {
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

            case DELIVERED, CANCELLED -> false;
        };

        if (!validTransition) {
            throw new IllegalArgumentException(
                    "Invalid status transition: "
                            + currentStatus
                            + " -> "
                            + newStatus
            );
        }

        order.setStatus(newStatus);

        return toResponse(orderRepository.save(order));
    }

    private OrderResponse toResponse(OrderEntity order) {

        AddressResponse address = new AddressResponse(
                order.getAddressId(),
                null,
                order.getRecipientName(),
                order.getPhone(),
                order.getAddressLine1(),
                order.getAddressLine2(),
                order.getCity(),
                order.getState(),
                order.getPostalCode(),
                order.getCountry(),
                false
        );

        List<OrderResponse.OrderItemResponse> items =
                order.getItems().stream()
                        .map(item ->
                                new OrderResponse.OrderItemResponse(
                                        item.getProductId(),
                                        item.getProductName(),
                                        item.getQuantity(),
                                        item.getUnitPrice(),
                                        item.getSubtotal()
                                )
                        )
                        .toList();

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                address,
                items
        );
    }
}
