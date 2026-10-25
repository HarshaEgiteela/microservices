
package com.example.productservice.service;

import com.example.productservice.entity.Product;
import com.example.productservice.repository.ProductRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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
        ProductService productService(ProductRepository repository) {
            return new ProductService(repository);
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

    @Test
    void firstRequestShouldFetchFromRepository() {

        when(productRepository.findAll())
                .thenReturn(List.of(createProduct()));

        List<Product> result = productService.getAllProducts();

        assertEquals(1, result.size());

        verify(productRepository, times(1)).findAll();

        assertNotNull(
                cacheManager.getCache("activeProducts").get("all")
        );
    }

    @Test
    void secondRequestShouldUseCache() {

        when(productRepository.findAll())
                .thenReturn(List.of(createProduct()));

        productService.getAllProducts();
        productService.getAllProducts();

        verify(productRepository, times(1)).findAll();
    }

    @Test
    void updatingProductShouldEvictCache() {

        Product product = createProduct();

        when(productRepository.findAll())
                .thenReturn(List.of(product));

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // First request populates cache.
        productService.getAllProducts();

        assertNotNull(
                cacheManager.getCache("activeProducts").get("all")
        );

        Product updated = createProduct();
        updated.setName("Updated Laptop");

        // Updating a product should evict the cached list.
        productService.updateProduct(1L, updated);

        assertNull(
                cacheManager.getCache("activeProducts").get("all")
        );

        // The next request must query the repository again.
        productService.getAllProducts();

        verify(productRepository, times(2)).findAll();
    }

    private Product createProduct() {

        Product product = new Product();
        product.setId(1L);
        product.setName("Laptop");
        product.setPrice(75000);
        product.setQuantity(10);
        product.setDescription("Test laptop");
        product.setActive(true);

        return product;
    }
}
