
package com.example.orderservice.service;

import com.example.orderservice.client.ProductClient;
import com.example.orderservice.dto.AddToCartRequest;
import com.example.orderservice.dto.CartResponse;
import com.example.orderservice.dto.Product;
import com.example.orderservice.entity.Cart;
import com.example.orderservice.entity.CartItem;
import com.example.orderservice.exception.ResourceNotFoundException;
import com.example.orderservice.repository.CartItemRepository;
import com.example.orderservice.repository.CartRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductClient productClient;

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId, String token) {

        Cart cart = cartRepository.findByUserId(userId)
                .orElse(null);

        if (cart == null) {
            return new CartResponse(
                    null,
                    userId,
                    List.of(),
                    BigDecimal.ZERO
            );
        }

        return buildResponse(cart, token);
    }

    @Transactional
    public CartResponse addItem(
            Long userId,
            AddToCartRequest request,
            String token
    ) {

        if (request.productId() == null ||
                request.productId() <= 0 ||
                request.quantity() == null ||
                request.quantity() <= 0) {

            throw new IllegalArgumentException(
                    "Valid product ID and quantity are required"
            );
        }

        Product product =
                productClient.getProduct(request.productId(), token);

        if (product.getQuantity() <= 0) {
            throw new IllegalArgumentException(
                    "This product is out of stock"
            );
        }

        Cart cart = getOrCreateCart(userId);

        CartItem item = cart.getItems().stream()
                .filter(existing ->
                        existing.getProductId()
                                .equals(request.productId()))
                .findFirst()
                .orElse(null);

        int currentQuantity =
                item == null ? 0 : item.getQuantity();

        long requestedQuantity =
                (long) currentQuantity + request.quantity();

        if (requestedQuantity > product.getQuantity()) {
            throw new IllegalArgumentException(
                    "Insufficient stock. Available: "
                            + product.getQuantity()
            );
        }

        if (item == null) {
            item = new CartItem();
            item.setProductId(request.productId());
            item.setQuantity(request.quantity());
            cart.addItem(item);
        } else {
            item.setQuantity((int) requestedQuantity);
        }

        cartRepository.saveAndFlush(cart);

        return buildResponse(cart, token);
    }

    @Transactional
    public CartResponse updateQuantity(
            Long userId,
            Long itemId,
            int quantity,
            String token
    ) {

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero"
            );
        }

        CartItem item = cartItemRepository
                .findByIdAndCartUserId(itemId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cart item not found: " + itemId
                        )
                );

        int currentQuantity = item.getQuantity();

        // Allow customers to reduce their cart quantity
        // even if the new quantity still exceeds stock.
        //
        // Example:
        // Cart quantity = 5
        // Available stock = 3
        //
        // 5 -> 4 : allowed
        // 4 -> 3 : allowed
        // 3 -> 4 : rejected

        if (quantity > currentQuantity) {

            Product product =
                    productClient.getProduct(
                            item.getProductId(),
                            token
                    );

            if (quantity > product.getQuantity()) {
                throw new IllegalArgumentException(
                        "Insufficient stock. Available: "
                                + product.getQuantity()
                );
            }
        }

        item.setQuantity(quantity);
        cartItemRepository.saveAndFlush(item);

        return buildResponse(item.getCart(), token);
    }

    @Transactional
    public void removeItem(Long userId, Long itemId) {

        CartItem item = cartItemRepository
                .findByIdAndCartUserId(itemId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cart item not found: " + itemId
                        )
                );

        Cart cart = item.getCart();
        cart.removeItem(item);
    }

    @Transactional
    public void clearCart(Long userId) {

        cartRepository.findByUserId(userId)
                .ifPresent(cart -> cart.getItems().clear());
    }

    private Cart getOrCreateCart(Long userId) {

        return cartRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUserId(userId);
                    return cartRepository.saveAndFlush(cart);
                });
    }

    private CartResponse buildResponse(
            Cart cart,
            String token
    ) {

        List<CartResponse.CartItemResponse> items =
                new ArrayList<>();

        BigDecimal total = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {

            Product product =
                    productClient.getProduct(
                            item.getProductId(),
                            token
                    );

            BigDecimal price =
                    BigDecimal.valueOf(product.getPrice());

            BigDecimal subtotal = price.multiply(
                    BigDecimal.valueOf(item.getQuantity())
            );

            total = total.add(subtotal);

            items.add(
                    new CartResponse.CartItemResponse(
                            item.getId(),
                            item.getProductId(),
                            product.getName(),
                            item.getQuantity(),
                            price,
                            subtotal
                    )
            );
        }

        return new CartResponse(
                cart.getId(),
                cart.getUserId(),
                items,
                total
        );
    }
}
