
package com.example.orderservice.config;

import com.example.orderservice.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
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

                .authorizeHttpRequests(auth -> auth

                        // Public endpoints
                        .requestMatchers(
                                "/actuator/health",
                                "/error"
                        ).permitAll()

                        // Admin: service instance information
                        .requestMatchers(
                                HttpMethod.GET,
                                "/orders/instance"
                        ).hasRole("ADMIN")

                        // Admin: update order status
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/orders/*/status"
                        ).hasRole("ADMIN")

                        // Admin: cancel any eligible order
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/orders/*/cancel"
                        ).hasRole("ADMIN")

                        // Admin: view all orders
                        .requestMatchers(
                                HttpMethod.GET,
                                "/orders"
                        ).hasRole("ADMIN")

                        // Customer or Admin: cancel own order
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/orders/my/*/cancel"
                        ).hasAnyRole("CUSTOMER", "ADMIN")

                        // Customer or Admin: cart operations
                        .requestMatchers(
                                "/cart",
                                "/cart/**"
                        ).hasAnyRole("CUSTOMER", "ADMIN")

                        // Customer or Admin: checkout and own orders
                        .requestMatchers(
                                "/orders/checkout",
                                "/orders/my",
                                "/orders/my/**"
                        ).hasAnyRole("CUSTOMER", "ADMIN")

                        // Deny everything else
                        .anyRequest().denyAll()
                )

                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
