
package com.example.orderservice.service;

import com.example.orderservice.client.ProductClient;
import com.example.orderservice.dto.*;
import com.example.orderservice.entity.*;
import com.example.orderservice.exception.ResourceNotFoundException;
import com.example.orderservice.repository.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private CartService cartService;

    private Product product(int stock) {
        Product p = new Product();
        p.setId(10L);
        p.setName("Mouse");
        p.setPrice(500.0);
        p.setQuantity(stock);
        return p;
    }

    @Test
    void emptyCartReturnsEmptyResponse() {
        when(cartRepository.findByUserId(5L))
                .thenReturn(Optional.empty());

        CartResponse response =
                cartService.getCart(5L, "token");

        assertTrue(response.items().isEmpty());
        assertEquals(5L, response.userId());
    }

    @Test
    void addItemCreatesCartItem() {
        Cart cart = new Cart();
        cart.setId(1L);
        cart.setUserId(5L);

        when(productClient.getProduct(10L, "token"))
                .thenReturn(product(10));

        when(cartRepository.findByUserIdForUpdate(5L))
                .thenReturn(Optional.of(cart));

        when(cartRepository.saveAndFlush(cart))
                .thenReturn(cart);

        CartResponse response = cartService.addItem(
                5L,
                new AddToCartRequest(10L, 2),
                "token"
        );

        assertEquals(1, response.items().size());
        assertEquals(2, response.items().get(0).quantity());
        assertEquals(2, cart.getItems().get(0).getQuantity());
    }

    @Test
    void addExistingProductIncreasesQuantity() {
        Cart cart = new Cart();
        cart.setId(1L);
        cart.setUserId(5L);

        CartItem item = new CartItem();
        item.setProductId(10L);
        item.setQuantity(2);
        cart.addItem(item);

        when(productClient.getProduct(10L, "token"))
                .thenReturn(product(10));

        when(cartRepository.findByUserIdForUpdate(5L))
                .thenReturn(Optional.of(cart));

        CartResponse response = cartService.addItem(
                5L,
                new AddToCartRequest(10L, 3),
                "token"
        );

        assertEquals(5, response.items().get(0).quantity());
        assertEquals(1, cart.getItems().size());
    }

    @Test
    void addItemRejectsInsufficientStock() {
        when(productClient.getProduct(10L, "token"))
                .thenReturn(product(2));

        Cart cart = new Cart();
        cart.setUserId(5L);

        when(cartRepository.findByUserIdForUpdate(5L))
                .thenReturn(Optional.of(cart));

        assertThrows(
                IllegalArgumentException.class,
                () -> cartService.addItem(
                        5L,
                        new AddToCartRequest(10L, 5),
                        "token"
                )
        );

        verify(cartRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateQuantityChangesItem() {
        Cart cart = new Cart();
        cart.setId(1L);
        cart.setUserId(5L);

        CartItem item = new CartItem();
        item.setId(20L);
        item.setProductId(10L);
        item.setQuantity(1);
        cart.addItem(item);

        when(cartItemRepository.findByIdAndCartUserId(20L, 5L))
                .thenReturn(Optional.of(item));

        when(productClient.getProduct(10L, "token"))
                .thenReturn(product(10));

        CartResponse response =
                cartService.updateQuantity(5L, 20L, 4, "token");

        assertEquals(4, response.items().get(0).quantity());
        verify(cartItemRepository).save(item);
    }

    @Test
    void removeItemRemovesOnlyOwnedItem() {
        Cart cart = new Cart();
        cart.setUserId(5L);

        CartItem item = new CartItem();
        item.setId(20L);
        item.setProductId(10L);
        cart.addItem(item);

        when(cartItemRepository.findByIdAndCartUserId(20L, 5L))
                .thenReturn(Optional.of(item));

        cartService.removeItem(5L, 20L);

        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void cannotRemoveAnotherUsersItem() {
        when(cartItemRepository.findByIdAndCartUserId(20L, 6L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> cartService.removeItem(6L, 20L)
        );
    }

    @Test
    void clearCartRemovesItems() {
        Cart cart = new Cart();
        cart.setUserId(5L);

        CartItem item = new CartItem();
        item.setProductId(10L);
        cart.addItem(item);

        when(cartRepository.findByUserId(5L))
                .thenReturn(Optional.of(cart));

        cartService.clearCart(5L);

        assertTrue(cart.getItems().isEmpty());
    }
}
