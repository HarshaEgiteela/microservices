
package com.example.productservice.service;

import com.example.productservice.entity.Product;
import com.example.productservice.repository.ProductRepository;
import com.example.productservice.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductPageCacheService {

    private final ProductRepository productRepository;

    @Cacheable(
            value = "activeProducts",
            key = "T(java.util.Arrays).asList(#search, #category, #minPrice, #maxPrice, #sort, #page, #size)"
    )
    @Transactional(readOnly = true)
    public CachedProductPage getPage(
            String search,
            String category,
            Double minPrice,
            Double maxPrice,
            String sort,
            int page,
            int size
    ) {

        String sortOption = sort == null ? "name_asc" : sort;

        Sort sorting = switch (sortOption) {

            case "price_asc" ->
                    Sort.by("price").ascending()
                            .and(Sort.by("id").ascending());

            case "price_desc" ->
                    Sort.by("price").descending()
                            .and(Sort.by("id").ascending());

            case "name_desc" ->
                    Sort.by("name").descending()
                            .and(Sort.by("id").ascending());

            case "name_asc" ->
                    Sort.by("name").ascending()
                            .and(Sort.by("id").ascending());

            default ->
                    throw new IllegalArgumentException(
                            "Invalid sort option: " + sortOption
                    );
        };

        Page<Product> result = productRepository.findAll(
                ProductSpecification.filter(
                        search,
                        category,
                        minPrice,
                        maxPrice
                ),
                PageRequest.of(page, size, sorting)
        );

        return new CachedProductPage(
                result.getContent(),
                result.getTotalElements()
        );
    }
}
