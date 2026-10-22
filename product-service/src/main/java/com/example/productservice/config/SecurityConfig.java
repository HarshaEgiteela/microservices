
package com.example.productservice.config;

import com.example.productservice.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(
                                new HttpStatusEntryPoint(
                                        HttpStatus.UNAUTHORIZED
                                )
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Public endpoints
                        .requestMatchers(
                                "/actuator/health",
                                "/error"
                        ).permitAll()

                        // Everyone can view products, even without login
                        .requestMatchers(
                                HttpMethod.GET,
                                "/products",
                                "/products/**"
                        ).permitAll()

                        // Temporary compatibility with Order Service checkout.
                        // Replace with service-to-service authentication.
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/products/*/reduce-stock"
                        ).hasAnyRole("CUSTOMER", "ADMIN")

                        // Stock restoration: admin-only interim rule.
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/products/*/increase-stock"
                        ).hasRole("ADMIN")

                        // Admin can create products
                        .requestMatchers(
                                HttpMethod.POST,
                                "/products"
                        ).hasRole("ADMIN")

                        // Admin can update products
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/products/**"
                        ).hasRole("ADMIN")

                        // Admin can delete products
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/products/**"
                        ).hasRole("ADMIN")

                        .anyRequest().denyAll()
                )

                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
