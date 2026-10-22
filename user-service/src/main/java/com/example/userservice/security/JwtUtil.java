
package com.example.userservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    // Generate JWT using the database user ID
    public String generateToken(Long userId, String role) {

        Date now = new Date();

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(now)
                .expiration(
                        new Date(now.getTime() + expirationMs)
                )
                .signWith(getSigningKey())
                .compact();
    }

    // Extract the user ID from the JWT subject
    public Long extractUserId(String token) {
        return Long.valueOf(
                getClaims(token).getSubject()
        );
    }

    public String extractRole(String token) {
        return getClaims(token)
                .get("role", String.class);
    }

    public boolean isValid(String token) {
        try {
            Claims claims = getClaims(token);

            Long userId = Long.valueOf(claims.getSubject());
            String role = claims.get("role", String.class);

            return userId > 0
                    && ("CUSTOMER".equals(role) || "ADMIN".equals(role));

        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}