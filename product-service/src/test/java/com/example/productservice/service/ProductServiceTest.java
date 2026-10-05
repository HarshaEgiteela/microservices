package com.example.productservice.service;

import com.example.productservice.entity.Product;
import com.example.productservice.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;


    // =========================================================
    // GET ALL PRODUCTS
    // =========================================================

    @Test
    void getAllProducts_shouldReturnAllProducts() {

        Product product1 = new Product();
        product1.setId(1L);
        product1.setName("Laptop");
        product1.setPrice(75000);
        product1.setQuantity(10);
        product1.setDescription("Test laptop");

        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("Phone");
        product2.setPrice(30000);
        product2.setQuantity(20);
        product2.setDescription("Test phone");

        when(productRepository.findAll())
                .thenReturn(List.of(product1, product2));

        List<Product> result =
                productService.getAllProducts();

        assertNotNull(result);
        assertEquals(2, result.size());

        assertEquals("Laptop", result.get(0).getName());
        assertEquals("Phone", result.get(1).getName());

        verify(productRepository)
                .findAll();
    }


    // =========================================================
    // GET PRODUCT BY ID
    // =========================================================

    @Test
    void getProductById_shouldReturnProductWhenFound() {

        Product product = new Product();

        product.setId(1L);
        product.setName("Laptop");
        product.setPrice(75000);
        product.setQuantity(10);
        product.setDescription("Test laptop");

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        Product result =
                productService.getProductById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Laptop", result.getName());
        assertEquals(75000, result.getPrice());
        assertEquals(10, result.getQuantity());

        verify(productRepository)
                .findById(1L);
    }


    @Test
    void getProductById_shouldThrowExceptionWhenNotFound() {

        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> productService.getProductById(999L)
                );

        assertEquals(
                "Product not found",
                exception.getMessage()
        );

        verify(productRepository)
                .findById(999L);
    }


    // =========================================================
    // CREATE PRODUCT
    // =========================================================

    @Test
    void createProduct_shouldSaveProduct() {

        Product product = new Product();

        product.setName("Laptop");
        product.setPrice(75000);
        product.setQuantity(10);
        product.setDescription("Test laptop");

        Product savedProduct = new Product();

        savedProduct.setId(1L);
        savedProduct.setName("Laptop");
        savedProduct.setPrice(75000);
        savedProduct.setQuantity(10);
        savedProduct.setDescription("Test laptop");

        when(productRepository.save(product))
                .thenReturn(savedProduct);

        Product result =
                productService.createProduct(product);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Laptop", result.getName());
        assertEquals(75000, result.getPrice());
        assertEquals(10, result.getQuantity());

        verify(productRepository)
                .save(product);
    }


    // =========================================================
    // UPDATE PRODUCT
    // =========================================================

    @Test
    void updateProduct_shouldUpdateProduct() {

        Product existingProduct = new Product();

        existingProduct.setId(1L);
        existingProduct.setName("Old Laptop");
        existingProduct.setPrice(60000);
        existingProduct.setQuantity(5);
        existingProduct.setDescription("Old description");

        Product updatedProduct = new Product();

        updatedProduct.setName("New Laptop");
        updatedProduct.setPrice(75000);
        updatedProduct.setQuantity(10);
        updatedProduct.setDescription("New description");

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(existingProduct));

        when(productRepository.save(existingProduct))
                .thenReturn(existingProduct);

        Product result =
                productService.updateProduct(
                        1L,
                        updatedProduct
                );

        assertNotNull(result);

        assertEquals("New Laptop", result.getName());
        assertEquals(75000, result.getPrice());
        assertEquals(10, result.getQuantity());
        assertEquals(
                "New description",
                result.getDescription()
        );

        verify(productRepository)
                .findById(1L);

        verify(productRepository)
                .save(existingProduct);
    }


    @Test
    void updateProduct_shouldThrowExceptionWhenProductNotFound() {

        Product updatedProduct = new Product();

        updatedProduct.setName("New Laptop");
        updatedProduct.setPrice(75000);
        updatedProduct.setQuantity(10);
        updatedProduct.setDescription("New description");

        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> productService.updateProduct(
                                999L,
                                updatedProduct
                        )
                );

        assertEquals(
                "Product not found",
                exception.getMessage()
        );

        verify(productRepository)
                .findById(999L);

        verify(productRepository, never())
                .save(any(Product.class));
    }


    // =========================================================
    // DELETE PRODUCT
    // =========================================================

    @Test
    void deleteProduct_shouldDeleteProduct() {

        Product product = new Product();

        product.setId(1L);
        product.setName("Laptop");

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        productService.deleteProduct(1L);

        verify(productRepository)
                .findById(1L);

        verify(productRepository)
                .delete(product);
    }


    @Test
    void deleteProduct_shouldThrowExceptionWhenNotFound() {

        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> productService.deleteProduct(999L)
                );

        assertEquals(
                "Product not found",
                exception.getMessage()
        );

        verify(productRepository)
                .findById(999L);

        verify(productRepository, never())
                .delete(any(Product.class));
    }


    // =========================================================
    // REDUCE STOCK
    // =========================================================

    @Test
    void reduceQuantity_shouldReduceStock() {

        Product product = new Product();

        product.setId(1L);
        product.setName("Laptop");
        product.setQuantity(10);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(productRepository.save(product))
                .thenReturn(product);

        Product result =
                productService.reduceQuantity(
                        1L,
                        3
                );

        assertNotNull(result);

        assertEquals(7, result.getQuantity());

        verify(productRepository)
                .findById(1L);

        verify(productRepository)
                .save(product);
    }


    @Test
    void reduceQuantity_shouldRejectZeroQuantity() {

        Product product = new Product();

        product.setId(1L);
        product.setQuantity(10);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> productService.reduceQuantity(
                                1L,
                                0
                        )
                );

        assertEquals(
                "Quantity must be greater than zero",
                exception.getMessage()
        );

        verify(productRepository)
                .findById(1L);

        verify(productRepository, never())
                .save(any(Product.class));
    }


    @Test
    void reduceQuantity_shouldRejectNegativeQuantity() {

        Product product = new Product();

        product.setId(1L);
        product.setQuantity(10);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> productService.reduceQuantity(
                                1L,
                                -1
                        )
                );

        assertEquals(
                "Quantity must be greater than zero",
                exception.getMessage()
        );

        verify(productRepository)
                .findById(1L);

        verify(productRepository, never())
                .save(any(Product.class));
    }


    @Test
    void reduceQuantity_shouldRejectInsufficientStock() {

        Product product = new Product();

        product.setId(1L);
        product.setQuantity(5);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> productService.reduceQuantity(
                                1L,
                                10
                        )
                );

        assertEquals(
                "Insufficient stock",
                exception.getMessage()
        );

        verify(productRepository)
                .findById(1L);

        verify(productRepository, never())
                .save(any(Product.class));
    }


    @Test
    void reduceQuantity_shouldThrowExceptionWhenProductNotFound() {

        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> productService.reduceQuantity(
                                999L,
                                2
                        )
                );

        assertEquals(
                "Product not found",
                exception.getMessage()
        );

        verify(productRepository)
                .findById(999L);

        verify(productRepository, never())
                .save(any(Product.class));
    }
}