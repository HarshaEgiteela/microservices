
package com.example.productservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RestoreStockRequest(

        @NotBlank(message = "Operation ID is required")
        String operationId,

        @NotNull(message = "Product ID is required")
        Long productId,

        @Min(value = 1, message = "Quantity must be greater than zero")
        int quantity
) {
}
