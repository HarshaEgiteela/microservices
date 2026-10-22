
package com.example.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Name cannot be empty")
        @Size(max = 255)
        String name,

        @NotBlank(message = "Email cannot be empty")
        @Email(message = "Invalid email address")
        @Size(max = 255)
        String email
) {}