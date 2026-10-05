package com.example.orderservice.controller;

import com.example.orderservice.entity.OrderEntity;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderRepository repository;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;


    @Test
    void getAllOrders_shouldReturnAllOrders() {

        OrderEntity order1 =
                new OrderEntity(
                        1L,
                        1L,
                        1L,
                        2,
                        OrderStatus.PLACED
                );

        OrderEntity order2 =
                new OrderEntity(
                        2L,
                        2L,
                        2L,
                        1,
                        OrderStatus.CONFIRMED
                );

        when(repository.findAll())
                .thenReturn(List.of(order1, order2));

        List<OrderEntity> result =
                orderController.getAllOrders();

        assertEquals(2, result.size());
        assertEquals(order1, result.get(0));
        assertEquals(order2, result.get(1));

        verify(repository).findAll();
    }


    @Test
    void placeOrder_shouldReturnSuccessResponse() {

        when(orderService.placeOrder(1L, 2L, 3))
                .thenReturn("Order placed successfully");

        ResponseEntity<String> response =
                orderController.placeOrder(
                        1L,
                        2L,
                        3
                );

        assertEquals(200, response.getStatusCode().value());
        assertEquals(
                "Order placed successfully",
                response.getBody()
        );

        verify(orderService)
                .placeOrder(1L, 2L, 3);
    }


    @Test
    void updateOrderStatus_shouldReturnUpdatedOrder() {

        OrderEntity order =
                new OrderEntity(
                        1L,
                        1L,
                        2L,
                        1,
                        OrderStatus.CONFIRMED
                );

        when(orderService.updateOrderStatus(
                1L,
                OrderStatus.CONFIRMED
        )).thenReturn(order);

        ResponseEntity<OrderEntity> response =
                orderController.updateOrderStatus(
                        1L,
                        OrderStatus.CONFIRMED
                );

        assertEquals(200, response.getStatusCode().value());
        assertEquals(order, response.getBody());

        verify(orderService)
                .updateOrderStatus(
                        1L,
                        OrderStatus.CONFIRMED
                );
    }


    @Test
    void getInstance_shouldReturnInstanceName() throws Exception {

        String result = orderController.getInstance();

        assertNotNull(result);
        assertTrue(
                result.startsWith("Order Service instance: ")
        );
    }
}