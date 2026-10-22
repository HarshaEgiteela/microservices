
package com.example.orderservice.client;

import com.example.orderservice.dto.Product;
import com.example.orderservice.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class ProductClient {

    private final RestTemplate restTemplate;

    private static final String PRODUCT_URL =
            "http://product-service/products";

    private static final String RESTORE_STOCK_URL =
            "http://product-service/internal/stock/restore";

    @Value("${internal.stock.secret}")
    private String internalStockSecret;

    // Get product details during checkout
    public Product getProduct(Long productId, String token) {

        try {
            ResponseEntity<Product> response =
                    restTemplate.exchange(
                            PRODUCT_URL + "/" + productId,
                            HttpMethod.GET,
                            authenticatedRequest(token),
                            Product.class
                    );

            Product product = response.getBody();

            if (product == null) {
                throw new ResourceNotFoundException(
                        "Product not found: " + productId
                );
            }

            return product;

        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException(
                    "Product not found: " + productId
            );
        }
    }

    // Reduce stock during checkout
    public void reduceStock(
            Long productId,
            int quantity,
            String token
    ) {

        try {
            restTemplate.exchange(
                    PRODUCT_URL + "/" + productId
                            + "/reduce-stock?quantity=" + quantity,
                    HttpMethod.PUT,
                    authenticatedRequest(token),
                    Void.class
            );

        } catch (HttpClientErrorException.BadRequest ex) {
            throw new IllegalArgumentException(
                    "Insufficient stock for product: " + productId
            );

        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException(
                    "Product not found: " + productId
            );
        }
    }

    // Restore stock when an order is cancelled.
    // Repeated calls with the same operationId must not
    // increase stock more than once.
    public void restoreStock(
            String operationId,
            Long productId,
            int quantity
    ) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Secret", internalStockSecret);

        RestoreStockRequest request = new RestoreStockRequest(
                operationId,
                productId,
                quantity
        );

        HttpEntity<RestoreStockRequest> entity =
                new HttpEntity<>(request, headers);

        try {
            restTemplate.exchange(
                    RESTORE_STOCK_URL,
                    HttpMethod.PUT,
                    entity,
                    Void.class
            );

        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException(
                    "Product not found: " + productId
            );
        }
    }

    // Attach customer/admin JWT for existing endpoints
    private HttpEntity<Void> authenticatedRequest(String token) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        return new HttpEntity<>(headers);
    }

    // JSON request sent to Product Service
    private record RestoreStockRequest(
            String operationId,
            Long productId,
            int quantity
    ) {
    }
}
