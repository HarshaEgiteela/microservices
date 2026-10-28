
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

    @Test
    void firstPageRequestShouldFetchFromRepository() {
        Product product = createProduct();

        when(productRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(product)));

        Page<Product> result = productService.searchProducts(
                null, null, null, null, "name_asc", 0, 20
        );

        assertEquals(1, result.getTotalElements());

        verify(productRepository, times(1)).findAll(
                any(Specification.class),
                any(Pageable.class)
        );

        assertNull(
                cacheManager.getCache("activeProducts").get("all")
        );
    }

    @Test
    void repeatedPageRequestsShouldQueryRepositoryEachTime() {
        when(productRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(createProduct())));

        productService.searchProducts(
                null, null, null, null, "name_asc", 0, 20
        );

        productService.searchProducts(
                null, null, null, null, "name_asc", 0, 20
        );

        verify(productRepository, times(2)).findAll(
                any(Specification.class),
                any(Pageable.class)
        );
    }

    @Test
    void updatingProductShouldEvictExistingCacheEntry() {
        Cache cache = cacheManager.getCache("activeProducts");
        assertNotNull(cache);

        cache.put("all", List.of(createProduct()));
        assertNotNull(cache.get("all"));

        Product existing = createProduct();

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Product updated = createProduct();
        updated.setName("Updated Laptop");

        productService.updateProduct(1L, updated);

        assertNull(cache.get("all"));
        assertEquals("Updated Laptop", existing.getName());

        verify(productRepository).save(existing);
    }

    @Test
    void creatingProductShouldEvictExistingCacheEntry() {
        Cache cache = cacheManager.getCache("activeProducts");
        assertNotNull(cache);

        cache.put("all", List.of(createProduct()));

        Product newProduct = createProduct();
        newProduct.setId(null);

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        productService.createProduct(newProduct);

        assertNull(cache.get("all"));
        verify(productRepository).save(any(Product.class));
    }
}
