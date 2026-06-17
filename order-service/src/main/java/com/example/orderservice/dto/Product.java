package com.example.orderservice.dto;

import lombok.Data;

@Data
public class Product {

    private Long id;
    private String name;
    private Double price;
}