
package com.example.productservice.service;

import com.example.productservice.entity.StockRestoration;
import com.example.productservice.exception.ResourceNotFoundException;
import com.example.productservice.repository.ProductRepository;
import com.example.productservice.repository.StockRestorationRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

                // Skip a restoration already completed.
                if (restorationRepository.existsByOperationId(operationId)) {
                    return;
                }

                // Reserve the operation ID.
                // The unique constraint prevents concurrent duplicates.
                StockRestoration restoration = new StockRestoration();
                restoration.setOperationId(operationId);
                restoration.setProductId(productId);
                restoration.setQuantity(quantity);

                restorationRepository.saveAndFlush(restoration);

                // Restore product stock in the same transaction.
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

            // Another request may have completed this operation.
            // Verify that before treating the retry as successful.
            if (!restorationRepository.existsByOperationId(operationId)) {
                throw ex;
            }
        }
    }
}
