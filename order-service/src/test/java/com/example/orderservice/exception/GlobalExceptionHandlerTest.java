package com.example.orderservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    @Test
    void shouldHandleResourceNotFoundException() {

        ResourceNotFoundException exception =
                new ResourceNotFoundException("Order not found: 999");

        ResponseEntity<String> response =
                handler.handleResourceNotFound(exception);

        assertEquals(
                HttpStatus.NOT_FOUND,
                response.getStatusCode()
        );

        assertEquals(
                "Order not found: 999",
                response.getBody()
        );
    }

    @Test
    void shouldHandleIllegalArgumentException() {

        IllegalArgumentException exception =
                new IllegalArgumentException(
                        "Quantity must be greater than zero"
                );

        ResponseEntity<String> response =
                handler.handleBadRequest(exception);

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        assertEquals(
                "Quantity must be greater than zero",
                response.getBody()
        );
    }

    @Test
    void shouldHandleUnexpectedException() {

        Exception exception =
                new Exception("Something went wrong");

        ResponseEntity<String> response =
                handler.handleException(exception);

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode()
        );

        assertEquals(
                "An unexpected error occurred",
                response.getBody()
        );
    }
}