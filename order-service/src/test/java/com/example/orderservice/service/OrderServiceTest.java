package com.example.orderservice.service;

import com.example.orderservice.dto.Product;
import com.example.orderservice.dto.User;
import com.example.orderservice.entity.OrderEntity;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.exception.ResourceNotFoundException;
import com.example.orderservice.repository.OrderRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private OrderService orderService;


    // =========================================================
    // PLACE ORDER
    // =========================================================

    @Test
    void placeOrder_shouldCreateOrderSuccessfully() {

        Long userId = 1L;
        Long productId = 2L;
        int quantity = 2;

        User user = new User();
        user.setId(userId);
        user.setName("Harsha");

        Product product = new Product();
        product.setId(productId);
        product.setName("Laptop");
        product.setQuantity(10);
        product.setPrice(75000);

        ResponseEntity<User> userResponse =
                ResponseEntity.ok(user);

        ResponseEntity<Product> productResponse =
                ResponseEntity.ok(product);

        when(restTemplate.exchange(
                eq("http://user-service/users/1"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(User.class)
        )).thenReturn(userResponse);

        when(restTemplate.exchange(
                eq("http://product-service/products/2"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Product.class)
        )).thenReturn(productResponse);

        when(restTemplate.exchange(
                eq("http://product-service/products/2/reduce-stock?quantity=2"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(Void.class)
        )).thenReturn(ResponseEntity.ok().build());

        String result =
                orderService.placeOrder(
                        userId,
                        productId,
                        quantity
                );

        assertEquals(
                "Order placed for Harsha | Product: Laptop | Quantity: 2",
                result
        );

        verify(restTemplate)
                .exchange(
                        eq("http://user-service/users/1"),
                        eq(HttpMethod.GET),
                        any(HttpEntity.class),
                        eq(User.class)
                );

        verify(restTemplate)
                .exchange(
                        eq("http://product-service/products/2"),
                        eq(HttpMethod.GET),
                        any(HttpEntity.class),
                        eq(Product.class)
                );

        verify(restTemplate)
                .exchange(
                        eq("http://product-service/products/2/reduce-stock?quantity=2"),
                        eq(HttpMethod.PUT),
                        any(HttpEntity.class),
                        eq(Void.class)
                );

        verify(orderRepository)
                .save(any(OrderEntity.class));
    }


    @Test
    void placeOrder_shouldRejectZeroQuantity() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> orderService.placeOrder(
                                1L,
                                2L,
                                0
                        )
                );

        assertEquals(
                "Quantity must be greater than zero",
                exception.getMessage()
        );

        verifyNoInteractions(restTemplate);
        verifyNoInteractions(orderRepository);
    }


    @Test
    void placeOrder_shouldRejectNegativeQuantity() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> orderService.placeOrder(
                                1L,
                                2L,
                                -1
                        )
                );

        assertEquals(
                "Quantity must be greater than zero",
                exception.getMessage()
        );

        verifyNoInteractions(restTemplate);
        verifyNoInteractions(orderRepository);
    }


    @Test
    void placeOrder_shouldRejectWhenUserNotFound() {

        HttpClientErrorException notFound =
                HttpClientErrorException.create(
                        HttpStatus.NOT_FOUND,
                        "Not Found",
                        null,
                        null,
                        null
                );

        when(restTemplate.exchange(
                eq("http://user-service/users/999"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(User.class)
        )).thenThrow(notFound);

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> orderService.placeOrder(
                                999L,
                                2L,
                                1
                        )
                );

        assertEquals(
                "User not found: 999",
                exception.getMessage()
        );

        verify(orderRepository, never())
                .save(any(OrderEntity.class));
    }


    @Test
    void placeOrder_shouldRejectWhenProductNotFound() {

        User user = new User();
        user.setId(1L);
        user.setName("Harsha");

        HttpClientErrorException notFound =
                HttpClientErrorException.create(
                        HttpStatus.NOT_FOUND,
                        "Not Found",
                        null,
                        null,
                        null
                );

        when(restTemplate.exchange(
                eq("http://user-service/users/1"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(User.class)
        )).thenReturn(ResponseEntity.ok(user));

        when(restTemplate.exchange(
                eq("http://product-service/products/999"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Product.class)
        )).thenThrow(notFound);

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> orderService.placeOrder(
                                1L,
                                999L,
                                1
                        )
                );

        assertEquals(
                "Product not found: 999",
                exception.getMessage()
        );

        verify(orderRepository, never())
                .save(any(OrderEntity.class));
    }


    @Test
    void placeOrder_shouldRejectInsufficientStock() {

        User user = new User();
        user.setId(1L);
        user.setName("Harsha");

        Product product = new Product();
        product.setId(2L);
        product.setName("Laptop");
        product.setQuantity(2);

        when(restTemplate.exchange(
                eq("http://user-service/users/1"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(User.class)
        )).thenReturn(ResponseEntity.ok(user));

        when(restTemplate.exchange(
                eq("http://product-service/products/2"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Product.class)
        )).thenReturn(ResponseEntity.ok(product));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> orderService.placeOrder(
                                1L,
                                2L,
                                5
                        )
                );

        assertEquals(
                "Insufficient stock. Available: 2",
                exception.getMessage()
        );

        verify(restTemplate, never())
                .exchange(
                        contains("reduce-stock"),
                        eq(HttpMethod.PUT),
                        any(HttpEntity.class),
                        eq(Void.class)
                );

        verify(orderRepository, never())
                .save(any(OrderEntity.class));
    }


    @Test
    void placeOrder_shouldSaveOrderWithPlacedStatus() {

        User user = new User();
        user.setId(1L);
        user.setName("Harsha");

        Product product = new Product();
        product.setId(2L);
        product.setName("Laptop");
        product.setQuantity(10);

        when(restTemplate.exchange(
                eq("http://user-service/users/1"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(User.class)
        )).thenReturn(ResponseEntity.ok(user));

        when(restTemplate.exchange(
                eq("http://product-service/products/2"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Product.class)
        )).thenReturn(ResponseEntity.ok(product));

        when(restTemplate.exchange(
                contains("reduce-stock"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(Void.class)
        )).thenReturn(ResponseEntity.ok().build());

        orderService.placeOrder(
                1L,
                2L,
                3
        );

        ArgumentCaptor<OrderEntity> captor =
                ArgumentCaptor.forClass(OrderEntity.class);

        verify(orderRepository)
                .save(captor.capture());

        OrderEntity savedOrder =
                captor.getValue();

        assertEquals(1L, savedOrder.getUserId());
        assertEquals(2L, savedOrder.getProductId());
        assertEquals(3, savedOrder.getQuantity());

        assertEquals(
                OrderStatus.PLACED,
                savedOrder.getStatus()
        );
    }


    // =========================================================
    // UPDATE ORDER STATUS
    // =========================================================

    @Test
    void updateOrderStatus_shouldChangePlacedToConfirmed() {

        OrderEntity order = new OrderEntity(
                1L,
                1L,
                2L,
                2,
                OrderStatus.PLACED
        );

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(order))
                .thenReturn(order);

        OrderEntity result =
                orderService.updateOrderStatus(
                        1L,
                        OrderStatus.CONFIRMED
                );

        assertEquals(
                OrderStatus.CONFIRMED,
                result.getStatus()
        );

        verify(orderRepository)
                .save(order);
    }


    @Test
    void updateOrderStatus_shouldChangeConfirmedToProcessing() {

        OrderEntity order = new OrderEntity(
                1L,
                1L,
                2L,
                2,
                OrderStatus.CONFIRMED
        );

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(order))
                .thenReturn(order);

        OrderEntity result =
                orderService.updateOrderStatus(
                        1L,
                        OrderStatus.PROCESSING
                );

        assertEquals(
                OrderStatus.PROCESSING,
                result.getStatus()
        );
    }


    @Test
    void updateOrderStatus_shouldChangeProcessingToShipped() {

        OrderEntity order = new OrderEntity(
                1L,
                1L,
                2L,
                2,
                OrderStatus.PROCESSING
        );

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(order))
                .thenReturn(order);

        OrderEntity result =
                orderService.updateOrderStatus(
                        1L,
                        OrderStatus.SHIPPED
                );

        assertEquals(
                OrderStatus.SHIPPED,
                result.getStatus()
        );
    }


    @Test
    void updateOrderStatus_shouldChangeShippedToDelivered() {

        OrderEntity order = new OrderEntity(
                1L,
                1L,
                2L,
                2,
                OrderStatus.SHIPPED
        );

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(order))
                .thenReturn(order);

        OrderEntity result =
                orderService.updateOrderStatus(
                        1L,
                        OrderStatus.DELIVERED
                );

        assertEquals(
                OrderStatus.DELIVERED,
                result.getStatus()
        );
    }


    @Test
    void updateOrderStatus_shouldAllowCancellationFromPlaced() {

        OrderEntity order = new OrderEntity(
                1L,
                1L,
                2L,
                2,
                OrderStatus.PLACED
        );

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(order))
                .thenReturn(order);

        OrderEntity result =
                orderService.updateOrderStatus(
                        1L,
                        OrderStatus.CANCELLED
                );

        assertEquals(
                OrderStatus.CANCELLED,
                result.getStatus()
        );
    }


    @Test
    void updateOrderStatus_shouldAllowCancellationFromConfirmed() {

        OrderEntity order = new OrderEntity(
                1L,
                1L,
                2L,
                2,
                OrderStatus.CONFIRMED
        );

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(order))
                .thenReturn(order);

        OrderEntity result =
                orderService.updateOrderStatus(
                        1L,
                        OrderStatus.CANCELLED
                );

        assertEquals(
                OrderStatus.CANCELLED,
                result.getStatus()
        );
    }


    @Test
    void updateOrderStatus_shouldAllowCancellationFromProcessing() {

        OrderEntity order = new OrderEntity(
                1L,
                1L,
                2L,
                2,
                OrderStatus.PROCESSING
        );

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(order))
                .thenReturn(order);

        OrderEntity result =
                orderService.updateOrderStatus(
                        1L,
                        OrderStatus.CANCELLED
                );

        assertEquals(
                OrderStatus.CANCELLED,
                result.getStatus()
        );
    }


    @Test
    void updateOrderStatus_shouldRejectInvalidTransition() {

        OrderEntity order = new OrderEntity(
                1L,
                1L,
                2L,
                2,
                OrderStatus.PLACED
        );

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> orderService.updateOrderStatus(
                                1L,
                                OrderStatus.SHIPPED
                        )
                );

        assertEquals(
                "Invalid status transition: PLACED → SHIPPED",
                exception.getMessage()
        );

        verify(orderRepository, never())
                .save(any(OrderEntity.class));
    }


    @Test
    void updateOrderStatus_shouldRejectDeliveredToPlaced() {

        OrderEntity order = new OrderEntity(
                1L,
                1L,
                2L,
                2,
                OrderStatus.DELIVERED
        );

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> orderService.updateOrderStatus(
                                1L,
                                OrderStatus.PLACED
                        )
                );

        assertEquals(
                "Invalid status transition: DELIVERED → PLACED",
                exception.getMessage()
        );

        verify(orderRepository, never())
                .save(any(OrderEntity.class));
    }


    @Test
    void updateOrderStatus_shouldRejectCancelledToConfirmed() {

        OrderEntity order = new OrderEntity(
                1L,
                1L,
                2L,
                2,
                OrderStatus.CANCELLED
        );

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> orderService.updateOrderStatus(
                                1L,
                                OrderStatus.CONFIRMED
                        )
                );

        assertEquals(
                "Invalid status transition: CANCELLED → CONFIRMED",
                exception.getMessage()
        );

        verify(orderRepository, never())
                .save(any(OrderEntity.class));
    }


    @Test
    void updateOrderStatus_shouldReturn404WhenOrderNotFound() {

        when(orderRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> orderService.updateOrderStatus(
                                999L,
                                OrderStatus.CONFIRMED
                        )
                );

        assertEquals(
                "Order not found: 999",
                exception.getMessage()
        );

        verify(orderRepository, never())
                .save(any(OrderEntity.class));
    }
}