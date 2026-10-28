
package com.example.userservice.service;

import com.example.userservice.dto.UpdateProfileRequest;
import com.example.userservice.dto.UserProfileResponse;
import com.example.userservice.entity.User;
import com.example.userservice.exception.ResourceNotFoundException;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.repository.AuthenticationRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;

    // NEW: To retrieve the user's role from the database
    private final AuthenticationRepository authenticationRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = findUser(userId);
        return toResponse(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(
            Long userId,
            UpdateProfileRequest request
    ) {
        User user = findUser(userId);

        String name = request.name().trim();
        String email = request.email().trim();

        if (!user.getEmail().equalsIgnoreCase(email)
                && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "Email already registered"
            );
        }

        user.setName(name);
        user.setEmail(email);

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException(
                    "Email already registered"
            );
        }

        return toResponse(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );
    }

    // UPDATED: Include role in profile response
    private UserProfileResponse toResponse(User user) {

        String role = authenticationRepository
                .findByUserId(user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Authentication record not found"
                        )
                )
                .getRole();

        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                role
        );
    }
}
