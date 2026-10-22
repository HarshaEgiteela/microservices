
package com.example.userservice.dto;

public record UserProfileResponse(
        Long id,
        String name,
        String email
) {}