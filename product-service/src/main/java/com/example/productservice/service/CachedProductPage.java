package com.example.productservice.service;

import com.example.productservice.entity.Product;
import java.util.List;

public record CachedProductPage(List<Product> content, long totalElements) { }
