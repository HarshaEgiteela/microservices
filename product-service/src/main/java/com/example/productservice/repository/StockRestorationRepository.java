
package com.example.productservice.repository;

import com.example.productservice.entity.StockRestoration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRestorationRepository
        extends JpaRepository<StockRestoration, Long> {

    boolean existsByOperationId(String operationId);
}
