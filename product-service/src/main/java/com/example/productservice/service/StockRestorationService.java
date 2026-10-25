
package com.example.productservice.service;

import com.example.productservice.entity.StockRestoration;
import com.example.productservice.exception.ResourceNotFoundException;
import com.example.productservice.repository.ProductRepository;
import com.example.productservice.repository.StockRestorationRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.cache.CacheManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class StockRestorationService {

    private final ProductRepository productRepository;
    private final StockRestorationRepository restorationRepository;
    private final TransactionTemplate transactionTemplate;
    private final CacheManager cacheManager;

    public void restoreStock(
            String operationId,
            Long productId,
            int quantity
    ) {

        if (operationId == null || operationId.isBlank()) {
            throw new IllegalArgumentException(
                    "Operation ID is required"
            );
        }

        if (productId == null || quantity <= 0) {
            throw new IllegalArgumentException(
                    "Valid product ID and quantity are required"
            );
        }

        try {
            transactionTemplate.executeWithoutResult(status -> {

                // Check for an existing restoration operation.
                var existing =
                        restorationRepository.findByOperationId(operationId);

                if (existing.isPresent()) {
                    verifyRestoration(
                            existing.get(),
                            productId,
                            quantity
                    );
                    return;
                }

                // Reserve the unique operation ID.
                StockRestoration restoration = new StockRestoration();
                restoration.setOperationId(operationId);
                restoration.setProductId(productId);
                restoration.setQuantity(quantity);

                restorationRepository.saveAndFlush(restoration);

                // Update MySQL inventory.
                int updatedRows = productRepository.increaseStock(
                        productId,
                        quantity
                );

                if (updatedRows == 0) {
                    throw new ResourceNotFoundException(
                            "Product not found: " + productId
                    );
                }

                // Schedule cache eviction after transaction commit.
                // RedisCacheManager is transaction-aware.
                var cache = cacheManager.getCache("activeProducts");

                if (cache != null) {
                    cache.clear();
                }
            });

        } catch (DataIntegrityViolationException ex) {

            // Another request may already have restored stock.
            StockRestoration existing =
                    restorationRepository.findByOperationId(operationId)
                            .orElseThrow(() -> ex);

            verifyRestoration(existing, productId, quantity);
        }
    }

    private void verifyRestoration(
            StockRestoration existing,
            Long productId,
            int quantity
    ) {

        if (!existing.getProductId().equals(productId)
                || existing.getQuantity() != quantity) {

            throw new IllegalStateException(
                    "Restoration operation ID already exists "
                            + "with different product or quantity: "
                            + existing.getOperationId()
            );
        }
    }
}
