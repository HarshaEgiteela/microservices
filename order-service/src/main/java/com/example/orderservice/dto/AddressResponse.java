
package com.example.orderservice.dto;

public record AddressResponse(
        Long id,
        String label,
        String recipientName,
        String phone,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country,
        boolean isDefault
) {}
