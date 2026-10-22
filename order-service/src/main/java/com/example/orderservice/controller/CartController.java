
package com.example.orderservice.controller;

import com.example.orderservice.dto.AddToCartRequest;
import com.example.orderservice.dto.CartResponse;
import com.example.orderservice.dto.UpdateCartItemRequest;
import com.example.orderservice.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    private Long userId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }

    private String token(String authorization) {
        if (authorization == null ||
                !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException(
                    "Bearer token is required"
            );
        }

        return authorization.substring(7);
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            Authentication authentication,
            @RequestHeader("Authorization") String authorization
    ) {
        return ResponseEntity.ok(
                cartService.getCart(
                        userId(authentication),
                        token(authorization)
                )
        );
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            Authentication authentication,
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody AddToCartRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(cartService.addItem(
                        userId(authentication),
                        request,
                        token(authorization)
                ));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> updateQuantity(
            Authentication authentication,
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        return ResponseEntity.ok(
                cartService.updateQuantity(
                        userId(authentication),
                        itemId,
                        request.quantity(),
                        token(authorization)
                )
        );
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeItem(
            Authentication authentication,
            @PathVariable Long itemId
    ) {
        cartService.removeItem(
                userId(authentication),
                itemId
        );

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(
            Authentication authentication
    ) {
        cartService.clearCart(userId(authentication));

        return ResponseEntity.noContent().build();
    }
}
