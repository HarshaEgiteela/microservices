
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private OrderService orderService;

    private OrderEntity order;

    @BeforeEach
    void setUp() {
        order = new OrderEntity();
        order.setId(1L);
        order.setUserId(5L);
        order.setStatus(OrderStatus.PLACED);
        order.setTotalAmount(BigDecimal.valueOf(1000));
    }

    @Test
    void getMyOrdersReturnsOnlyCustomerOrders() {
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(5L))
                .thenReturn(List.of(order));

        List<OrderResponse> result =
                orderService.getMyOrders(5L);

        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).userId());

        verify(orderRepository)
                .findByUserIdOrderByCreatedAtDesc(5L);
    }

    @Test
    void getMyOrderReturnsMatchingOrder() {
        when(orderRepository.findByIdAndUserId(1L, 5L))
                .thenReturn(Optional.of(order));

        OrderResponse response =
                orderService.getMyOrder(5L, 1L);

        assertEquals(1L, response.id());
        assertEquals(5L, response.userId());
    }

    @Test
    void getMyOrderRejectsAnotherUsersOrder() {
        when(orderRepository.findByIdAndUserId(1L, 6L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.getMyOrder(6L, 1L)
        );
    }

    @Test
    void checkoutRejectsMissingCart() {
        when(cartRepository.findByUserIdForUpdate(5L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.checkout(5L, 3L, "token")
        );

        verifyNoInteractions(productClient, userClient);
    }

    @Test
    void checkoutRejectsEmptyCart() {
        Cart cart = new Cart();
        cart.setUserId(5L);

        when(cartRepository.findByUserIdForUpdate(5L))
                .thenReturn(Optional.of(cart));

        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.checkout(5L, 3L, "token")
        );

        verifyNoInteractions(productClient, userClient);
    }

    @Test
    void checkoutCreatesOrderAndClearsCart() {
        Cart cart = new Cart();
        cart.setId(10L);
        cart.setUserId(5L);

        CartItem item = new CartItem();
        item.setId(20L);
        item.setProductId(100L);
        item.setQuantity(2);
        cart.addItem(item);

        Product product = new Product();
        product.setId(100L);
        product.setName("Keyboard");
        product.setPrice(500.0);
        product.setQuantity(10);

        AddressResponse address = new AddressResponse(
                3L, "Home", "Harsha", "9999999999",
                "Street 1", null, "Chennai",
                "Tamil Nadu", "600001", "India", true
        );

        when(cartRepository.findByUserIdForUpdate(5L))
                .thenReturn(Optional.of(cart));

        when(userClient.getAddress(3L, "token"))
                .thenReturn(address);

        when(productClient.getProduct(100L, "token"))
                .thenReturn(product);

        when(orderRepository.save(any(OrderEntity.class)))
                .thenAnswer(invocation -> {
                    OrderEntity saved = invocation.getArgument(0);
                    saved.setId(1L);
                    return saved;
                });

        OrderResponse result =
                orderService.checkout(5L, 3L, "token");

        assertEquals(1L, result.id());
        assertEquals(5L, result.userId());
        assertEquals(
                0,
                result.totalAmount()
                        .compareTo(BigDecimal.valueOf(1000))
        );
        assertEquals(1, result.items().size());
        assertEquals("Chennai", result.deliveryAddress().city());
        assertTrue(cart.getItems().isEmpty());

        verify(productClient).reduceStock(100L, 2, "token");
        verify(orderRepository).save(any(OrderEntity.class));
    }

    @Test
    void checkoutRejectsInsufficientStock() {
        Cart cart = new Cart();
        cart.setUserId(5L);

        CartItem item = new CartItem();
        item.setProductId(100L);
        item.setQuantity(5);
        cart.addItem(item);

        Product product = new Product();
        product.setId(100L);
        product.setName("Mouse");
        product.setPrice(200.0);
        product.setQuantity(2);

        when(cartRepository.findByUserIdForUpdate(5L))
                .thenReturn(Optional.of(cart));

        when(userClient.getAddress(eq(3L), anyString()))
                .thenReturn(new AddressResponse(
                        3L, "Home", "Harsha", "9999999999",
                        "Street", null, "Chennai",
                        "TN", "600001", "India", true
                ));

        when(productClient.getProduct(100L, "token"))
                .thenReturn(product);

        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.checkout(5L, 3L, "token")
        );

        verify(productClient, never())
                .reduceStock(anyLong(), anyInt(), anyString());

        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateStatusFromPlacedToConfirmed() {
        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(any(OrderEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse result =
                orderService.updateOrderStatus(
                        1L, OrderStatus.CONFIRMED
                );

        assertEquals(OrderStatus.CONFIRMED, result.status());
    }

    @Test
    void rejectInvalidStatusTransition() {
        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.updateOrderStatus(
                        1L, OrderStatus.DELIVERED
                )
        );

        verify(orderRepository, never()).save(any());
    }

    @Test
    void deliveredOrderCannotChangeStatus() {
        order.setStatus(OrderStatus.DELIVERED);

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.updateOrderStatus(
                        1L, OrderStatus.CANCELLED
                )
        );
    }

    @Test
    void updateMissingOrderThrowsNotFound() {
        when(orderRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.updateOrderStatus(
                        99L, OrderStatus.CONFIRMED
                )
        );
    }
}
