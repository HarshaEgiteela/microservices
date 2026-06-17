package com.example.productservice.controller;

import com.example.productservice.entity.Product;
import com.example.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository repository;

    @GetMapping
    public List<Product> getAll() {
        return repository.findAll();
    }

    @PostMapping
    public Product save(@RequestBody Product product) {
        return repository.save(product);
    }

    @GetMapping("/{id}")
    public Product getById(@PathVariable Long id) {
        return repository.findById(id).orElse(null);
    }
}