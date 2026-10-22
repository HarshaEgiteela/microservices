
package com.example.productservice.controller;

import com.example.productservice.service.StockRestorationService;
import lombok.RequiredArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/stock")
@RequiredArgsConstructor
public class InternalStockController {

    private final StockRestorationService stockRestorationService;

    public record RestoreStockRequest(
            @NotBlank String operationId,
            @NotNull Long productId,
            @Positive int quantity
    ) {}

    @PostMapping("/restore")
    public ResponseEntity<Void> restoreStock(
            @Valid @RequestBody RestoreStockRequest request
    ) {

        stockRestorationService.restoreStock(
                request.operationId(),
                request.productId(),
                request.quantity()
        );

        return ResponseEntity.noContent().build();
    }
}
