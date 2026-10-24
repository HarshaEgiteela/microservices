
package com.example.productservice.repository;

import com.example.productservice.entity.StockRestoration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockRestorationRepository
        extends JpaRepository<StockRestoration, Long> {

    boolean existsByOperationId(String operationId);

    Optional<StockRestoration> findByOperationId(String operationId);
}
