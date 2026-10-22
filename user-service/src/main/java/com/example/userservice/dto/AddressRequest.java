
package com.example.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(

        @NotBlank String label,

        @NotBlank String recipientName,

        @NotBlank
        @Size(max = 20)
        String phone,

        @NotBlank String addressLine1,

        String addressLine2,

        @NotBlank String city,

        @NotBlank String state,

        @NotBlank String postalCode,

        @NotBlank String country
) {}