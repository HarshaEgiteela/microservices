
package com.example.orderservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    @Test
    void notFoundReturns404() {
        ResponseEntity<Map<String, String>> response =
                handler.handleNotFound(
                        new ResourceNotFoundException("Not found")
                );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Not found", response.getBody().get("message"));
    }

    @Test
    void invalidRequestReturns400() {
        ResponseEntity<Map<String, String>> response =
                handler.handleBadRequest(
                        new IllegalArgumentException("Invalid quantity")
                );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(
                "Invalid quantity",
                response.getBody().get("message")
        );
    }

    @Test
    void unavailableServiceReturns503() {
        ResponseEntity<Map<String, String>> response =
                handler.handleUnavailable(
                        new ResourceAccessException("Connection refused")
                );

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                response.getStatusCode()
        );
    }

    @Test
    void unexpectedExceptionReturns500() {
        ResponseEntity<Map<String, String>> response =
                handler.handleUnexpected(
                        new RuntimeException("Database error")
                );

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode()
        );
        assertEquals(
                "An unexpected error occurred",
                response.getBody().get("message")
        );
    }
}
