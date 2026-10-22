
package com.example.productservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class InternalStockAuthFilter extends OncePerRequestFilter {

    @Value("${internal.stock.secret}")
    private String internalStockSecret;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith("/internal/stock/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String providedSecret =
                request.getHeader("X-Internal-Secret");

        if (internalStockSecret == null
                || internalStockSecret.isBlank()
                || providedSecret == null
                || !MessageDigest.isEqual(
                internalStockSecret.getBytes(StandardCharsets.UTF_8),
                providedSecret.getBytes(StandardCharsets.UTF_8)
        )) {

            response.sendError(
                    HttpStatus.FORBIDDEN.value(),
                    "Unauthorized internal stock operation"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}
