package com.example.productservice.config;

import com.example.productservice.security.JwtAuthFilter;
import com.example.productservice.security.InternalStockAuthFilter;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
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
    private final InternalStockAuthFilter internalStockAuthFilter;

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

                        // Health checks
                        .requestMatchers(
                                "/actuator/health",
                                "/error"
                        ).permitAll()

                        // Public product viewing
                        .requestMatchers(
                                HttpMethod.GET,
                                "/products",
                                "/products/**"
                        ).permitAll()

                        // Internal stock restoration
                        // InternalStockAuthFilter checks the secret
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/internal/stock/restore"
                        ).permitAll()

                        // Checkout stock reduction
                        // Temporary until we also move reduction
                        // to service-to-service authentication
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/products/*/reduce-stock"
                        ).hasAnyRole("CUSTOMER", "ADMIN")

                        // Old stock restoration API must not be used
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/products/*/increase-stock"
                        ).denyAll()

                        // Admin product creation
                        .requestMatchers(
                                HttpMethod.POST,
                                "/products"
                        ).hasRole("ADMIN")

                        // Admin product updates
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/products/**"
                        ).hasRole("ADMIN")

                        // Admin product deletion
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/products/**"
                        ).hasRole("ADMIN")

                        .anyRequest().denyAll()
                )

                // Internal service authentication
                .addFilterBefore(
                        internalStockAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                // JWT authentication
                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    // Prevent InternalStockAuthFilter from also being
    // registered automatically as a servlet filter.
    @Bean
    public FilterRegistrationBean<InternalStockAuthFilter>
    internalStockFilterRegistration(
            InternalStockAuthFilter filter
    ) {

        FilterRegistrationBean<InternalStockAuthFilter> registration =
                new FilterRegistrationBean<>(filter);

        registration.setEnabled(false);

        return registration;
    }

    // Prevent JwtAuthFilter from also being
    // registered automatically as a servlet filter.
    @Bean
    public FilterRegistrationBean<JwtAuthFilter>
    jwtFilterRegistration(
            JwtAuthFilter filter
    ) {

        FilterRegistrationBean<JwtAuthFilter> registration =
                new FilterRegistrationBean<>(filter);

        registration.setEnabled(false);

        return registration;
    }
}