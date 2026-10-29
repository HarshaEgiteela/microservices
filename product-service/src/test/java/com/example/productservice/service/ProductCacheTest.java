
package com.example.productservice.service;

import com.example.productservice.entity.Product;
import com.example.productservice.repository.ProductRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(ProductCacheTest.TestConfig.class)
class ProductCacheTest {

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        ProductRepository productRepository() {
            return mock(ProductRepository.class);
        }

        @Bean
        ProductPageCacheService productPageCacheService(
                ProductRepository repository) {
            return new ProductPageCacheService(repository);
        }

        @Bean
        ProductService productService(
                ProductRepository repository,
                ProductPageCacheService pageCacheService) {
            return new ProductService(repository, pageCacheService);
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("activeProducts");
        }
    }

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        reset(productRepository);
        cacheManager.getCache("activeProducts").clear();
    }

    private Product createProduct() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Laptop");
        product.setPrice(75000);
        product.setQuantity(10);
        product.setDescription("Test laptop");
        product.setCategory("Electronics");
        product.setActive(true);
        return product;
    }

    private void mockProductPage() {
        when(productRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(
                new PageImpl<>(List.of(createProduct()))
        );
    }

    @Test
    void firstPageRequestShouldFetchFromRepository() {
        mockProductPage();

        Page<Product> result = productService.searchProducts(
                null, null, null, null, "name_asc", 0, 20
        );

        assertEquals(1, result.getTotalElements());

        verify(productRepository, times(1)).findAll(
                any(Specification.class),
                any(Pageable.class)
        );
    }

    @Test
    void repeatedPageRequestsShouldUseCache() {
        mockProductPage();

        productService.searchProducts(
                null, null, null, null, "name_asc", 0, 20
        );

        productService.searchProducts(
                null, null, null, null, "name_asc", 0, 20
        );

        verify(productRepository, times(1)).findAll(
                any(Specification.class),
                any(Pageable.class)
        );
    }

    @Test
    void differentPagesShouldHaveDifferentCacheEntries() {
        mockProductPage();

        productService.searchProducts(
                null, null, null, null, "name_asc", 0, 20
        );

        productService.searchProducts(
                null, null, null, null, "name_asc", 1, 20
        );

        verify(productRepository, times(2)).findAll(
                any(Specification.class),
                any(Pageable.class)
        );
    }

    @Test
    void updatingProductShouldEvictCache() {
        mockProductPage();

        productService.searchProducts(
                null, null, null, null, "name_asc", 0, 20
        );

        Product existing = createProduct();

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        Product updated = createProduct();
        updated.setName("Updated Laptop");

        productService.updateProduct(1L, updated);

        assertEquals("Updated Laptop", existing.getName());

        productService.searchProducts(
                null, null, null, null, "name_asc", 0, 20
        );

        verify(productRepository, times(2)).findAll(
                any(Specification.class),
                any(Pageable.class)
        );

        verify(productRepository).save(existing);
    }

    @Test
    void creatingProductShouldEvictCache() {
        mockProductPage();

        productService.searchProducts(
                null, null, null, null, "name_asc", 0, 20
        );

        Product newProduct = createProduct();
        newProduct.setId(null);

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        productService.createProduct(newProduct);

        productService.searchProducts(
                null, null, null, null, "name_asc", 0, 20
        );

        verify(productRepository, times(2)).findAll(
                any(Specification.class),
                any(Pageable.class)
        );
    }
}
