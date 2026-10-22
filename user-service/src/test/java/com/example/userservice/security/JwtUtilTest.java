
package com.example.userservice.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();

        ReflectionTestUtils.setField(
                jwtUtil,
                "secret",
                "ThisIsATestSigningSecretWithMoreThan48CharactersForHS384Security"
        );

        ReflectionTestUtils.setField(
                jwtUtil,
                "expirationMs",
                3600000L
        );
    }

    @Test
    void generateToken_shouldContainUserIdAndRole() {

        String token = jwtUtil.generateToken(
                5L,
                "CUSTOMER"
        );

        assertNotNull(token);
        assertTrue(jwtUtil.isValid(token));

        assertEquals(
                5L,
                jwtUtil.extractUserId(token)
        );

        assertEquals(
                "CUSTOMER",
                jwtUtil.extractRole(token)
        );
    }

    @Test
    void generateToken_shouldSupportAdminRole() {

        String token = jwtUtil.generateToken(
                10L,
                "ADMIN"
        );

        assertEquals(
                10L,
                jwtUtil.extractUserId(token)
        );

        assertEquals(
                "ADMIN",
                jwtUtil.extractRole(token)
        );
    }

    @Test
    void isValid_shouldRejectInvalidToken() {

        assertFalse(
                jwtUtil.isValid("invalid.jwt.token")
        );
    }

    @Test
    void isValid_shouldRejectExpiredToken() {

        ReflectionTestUtils.setField(
                jwtUtil,
                "expirationMs",
                -1000L
        );

        String token = jwtUtil.generateToken(
                5L,
                "CUSTOMER"
        );

        assertFalse(jwtUtil.isValid(token));
    }
}
