package com.example.userservice.service;

import com.example.userservice.dto.AuthResponse;
import com.example.userservice.entity.Authentication;
import com.example.userservice.entity.User;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.repository.AuthenticationRepository;
import com.example.userservice.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationRepository authenticationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;


    // =========================
    // REGISTER
    // =========================

    public User register(
            String name,
            String email,
            String password) {

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Name cannot be empty");
        }

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Email cannot be empty");
        }

        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException(
                    "Password cannot be empty");
        }

        email = email.trim();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "Email already registered");
        }

        User user = new User();

        user.setName(name.trim());
        user.setEmail(email);

        User savedUser = userRepository.save(user);

        Authentication authentication =
                new Authentication();

        authentication.setUser(savedUser);

        authentication.setPasswordHash(
                passwordEncoder.encode(password)
        );

        authentication.setRole("CUSTOMER");

        authenticationRepository.save(authentication);

        return savedUser;
    }


    // =========================
    // LOGIN
    // =========================

    public AuthResponse login(
            String email,
            String password) {

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Email cannot be empty");
        }

        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException(
                    "Password cannot be empty");
        }

        email = email.trim();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid email or password")
                );

        Authentication authentication =
                authenticationRepository
                        .findByUserId(user.getId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invalid email or password")
                        );

        boolean passwordMatches =
                passwordEncoder.matches(
                        password,
                        authentication.getPasswordHash()
                );

        if (!passwordMatches) {
            throw new IllegalArgumentException(
                    "Invalid email or password");
        }

        String token = jwtUtil.generateToken(
                user.getEmail(),
                authentication.getRole()
        );

        return new AuthResponse(
                token,
                authentication.getRole()
        );
    }
}