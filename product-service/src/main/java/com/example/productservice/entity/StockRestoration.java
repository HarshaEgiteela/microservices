
package com.example.productservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "stock_restorations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_stock_restoration_operation",
                        columnNames = "operation_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class StockRestoration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "operation_id",
            nullable = false,
            unique = true,
            length = 150
    )
    private String operationId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
