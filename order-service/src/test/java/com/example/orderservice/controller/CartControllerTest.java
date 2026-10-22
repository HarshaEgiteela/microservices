
package com.example.orderservice.controller;

import com.example.orderservice.dto.*;
import com.example.orderservice.service.CartService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartController controller;

    private UsernamePasswordAuthenticationToken auth() {
        return new UsernamePasswordAuthenticationToken(
                5L, null, List.of()
        );
    }

    @Test
    void getCartUsesJwtUserId() {
        controller.getCart(auth(), "Bearer token");

        verify(cartService).getCart(5L, "token");
    }

    @Test
    void addItemUsesJwtUserId() {
        AddToCartRequest request =
                new AddToCartRequest(10L, 2);

        assertEquals(
                201,
                controller.addItem(
                        auth(), "Bearer token", request
                ).getStatusCode().value()
        );

        verify(cartService).addItem(
                5L, request, "token"
        );
    }

    @Test
    void updateQuantityUsesJwtUserId() {
        UpdateCartItemRequest request =
                new UpdateCartItemRequest(4);

        controller.updateQuantity(
                auth(), "Bearer token", 20L, request
        );

        verify(cartService).updateQuantity(
                5L, 20L, 4, "token"
        );
    }

    @Test
    void removeItemReturnsNoContent() {
        assertEquals(
                204,
                controller.removeItem(auth(), 20L)
                        .getStatusCode().value()
        );

        verify(cartService).removeItem(5L, 20L);
    }

    @Test
    void clearCartReturnsNoContent() {
        assertEquals(
                204,
                controller.clearCart(auth())
                        .getStatusCode().value()
        );

        verify(cartService).clearCart(5L);
    }

    @Test
    void missingBearerTokenIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> controller.getCart(auth(), "invalid")
        );
    }
}
