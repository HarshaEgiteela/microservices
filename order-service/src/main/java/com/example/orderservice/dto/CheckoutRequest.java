
package com.example.orderservice.dto;

import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(

        @NotNull(message = "Delivery address is required")
        Long addressId

) {}
