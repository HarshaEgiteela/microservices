
package com.example.productservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product name is required")
    @Size(max = 255)
    @Column(nullable = false)
    private String name;

    @PositiveOrZero(message = "Price cannot be negative")
    @Column(nullable = false)
    private double price;

    @PositiveOrZero(message = "Quantity cannot be negative")
    @Column(nullable = false)
    private int quantity;

    @Size(max = 2000)
    @Column(length = 2000)
    private String description;
}
