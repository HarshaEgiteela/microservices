
package com.example.orderservice.client;

import com.example.orderservice.dto.Product;
import com.example.orderservice.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class ProductClient {

    private final RestTemplate restTemplate;

    public Product getProduct(Long productId, String token) {

        try {
            ResponseEntity<Product> response =
                    restTemplate.exchange(
                            "http://product-service/products/" + productId,
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

    public void reduceStock(
            Long productId,
            int quantity,
            String token
    ) {
        try {
            restTemplate.exchange(
                    "http://product-service/products/"
                            + productId
                            + "/reduce-stock?quantity="
                            + quantity,
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

    private HttpEntity<Void> authenticatedRequest(String token) {

        HttpHeaders headers = new HttpHeaders();

        headers.setBearerAuth(token);

        return new HttpEntity<>(headers);
    }
}
