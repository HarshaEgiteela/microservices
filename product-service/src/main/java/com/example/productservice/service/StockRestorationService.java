
package com.example.productservice.service;

import com.example.productservice.entity.StockRestoration;
import com.example.productservice.exception.ResourceNotFoundException;
import com.example.productservice.repository.ProductRepository;
import com.example.productservice.repository.StockRestorationRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class StockRestorationService {

    private final ProductRepository productRepository;
    private final StockRestorationRepository restorationRepository;
    private final TransactionTemplate transactionTemplate;

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

                // Check whether this restoration already exists.
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

                // Increase stock in the same transaction.
                int updatedRows = productRepository.increaseStock(
                        productId,
                        quantity
                );

                if (updatedRows == 0) {
                    throw new ResourceNotFoundException(
                            "Product not found: " + productId
                    );
                }
            });

        } catch (DataIntegrityViolationException ex) {

            // A concurrent request may have committed this operation.
            // Verify the stored details before accepting it as successful.
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
