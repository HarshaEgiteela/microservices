
package com.example.orderservice.controller;

import com.example.orderservice.dto.CheckoutRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.service.OrderService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController controller;

    private UsernamePasswordAuthenticationToken auth() {
        return new UsernamePasswordAuthenticationToken(
                5L, null, List.of()
        );
    }

    @Test
    void checkoutUsesAuthenticatedUserId() {
        controller.checkout(
                auth(),
                "Bearer example-token",
                new CheckoutRequest(3L)
        );

        verify(orderService).checkout(
                5L, 3L, "example-token"
        );
    }

    @Test
    void checkoutReturnsCreated() {
        ResponseEntity<OrderResponse> response =
                controller.checkout(
                        auth(),
                        "Bearer example-token",
                        new CheckoutRequest(3L)
                );

        assertEquals(201, response.getStatusCode().value());
    }

    @Test
    void getMyOrdersUsesAuthenticatedUser() {
        when(orderService.getMyOrders(5L))
                .thenReturn(List.of());

        assertEquals(
                200,
                controller.getMyOrders(auth())
                        .getStatusCode().value()
        );

        verify(orderService).getMyOrders(5L);
    }

    @Test
    void getMyOrderUsesAuthenticatedUser() {
        controller.getMyOrder(auth(), 10L);

        verify(orderService).getMyOrder(5L, 10L);
    }

    @Test
    void getAllOrdersDelegatesToService() {
        when(orderService.getAllOrders())
                .thenReturn(List.of());

        assertEquals(
                200,
                controller.getAllOrders()
                        .getStatusCode().value()
        );
    }

    @Test
    void updateOrderStatusDelegatesToService() {
        controller.updateOrderStatus(
                10L,
                OrderStatus.CONFIRMED
        );

        verify(orderService).updateOrderStatus(
                10L,
                OrderStatus.CONFIRMED
        );
    }
}
