
package com.example.orderservice.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(

        Long cartId,
        Long userId,
        List<CartItemResponse> items,
        BigDecimal totalAmount

) {
    public record CartItemResponse(
            Long itemId,
            Long productId,
            String productName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal
    ) {}
}
