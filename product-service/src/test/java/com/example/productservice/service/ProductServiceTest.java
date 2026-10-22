
package com.example.productservice.service;

import com.example.productservice.entity.Product;
import com.example.productservice.exception.ResourceNotFoundException;
import com.example.productservice.repository.ProductRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setName("Laptop");
        product.setPrice(75000);
        product.setQuantity(10);
        product.setDescription("Test laptop");
        product.setActive(true);
    }

    // ==========================================
    // GET ALL PRODUCTS
    // ==========================================

    @Test
    void getAllProducts_shouldReturnAllProducts() {

        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("Phone");
        product2.setPrice(30000);
        product2.setQuantity(20);
        product2.setActive(true);

        when(productRepository.findAll())
                .thenReturn(List.of(product, product2));

        List<Product> result =
                productService.getAllProducts();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Laptop", result.get(0).getName());
        assertEquals("Phone", result.get(1).getName());

        verify(productRepository).findAll();
    }

    @Test
    void getAllProducts_shouldExcludeInactiveProducts() {

        Product inactiveProduct = new Product();
        inactiveProduct.setId(2L);
        inactiveProduct.setName("Deleted Phone");
        inactiveProduct.setActive(false);

        when(productRepository.findAll())
                .thenReturn(List.of(product, inactiveProduct));

        List<Product> result =
                productService.getAllProducts();

        assertEquals(1, result.size());
        assertEquals("Laptop", result.get(0).getName());
        assertTrue(result.get(0).isActive());

        verify(productRepository).findAll();
    }

    // ==========================================
    // GET PRODUCT BY ID
    // ==========================================

    @Test
    void getProductById_shouldReturnProductWhenFound() {

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        Product result =
                productService.getProductById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Laptop", result.getName());
        assertEquals(75000, result.getPrice());
        assertEquals(10, result.getQuantity());

        verify(productRepository).findById(1L);
    }

    @Test
    void getProductById_shouldThrowWhenNotFound() {

        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> productService.getProductById(999L)
                );

        assertEquals(
                "Product not found: 999",
                exception.getMessage()
        );

        verify(productRepository).findById(999L);
    }

    // ==========================================
    // CREATE PRODUCT
    // ==========================================

    @Test
    void createProduct_shouldSaveProduct() {

        Product savedProduct = new Product();
        savedProduct.setId(1L);
        savedProduct.setName("Laptop");
        savedProduct.setPrice(75000);
        savedProduct.setQuantity(10);
        savedProduct.setDescription("Test laptop");
        savedProduct.setActive(true);

        when(productRepository.save(any(Product.class)))
                .thenReturn(savedProduct);

        Product result =
                productService.createProduct(product);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Laptop", result.getName());
        assertEquals(75000, result.getPrice());
        assertTrue(result.isActive());

        verify(productRepository).save(product);

        // The database generates the ID.
        assertNull(product.getId());
    }

    @Test
    void createProduct_shouldRejectBlankName() {

        product.setName("   ");

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.createProduct(product)
        );

        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_shouldRejectNegativePrice() {

        product.setPrice(-100);

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.createProduct(product)
        );

        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_shouldRejectNegativeQuantity() {

        product.setQuantity(-5);

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.createProduct(product)
        );

        verify(productRepository, never()).save(any());
    }

    // ==========================================
    // UPDATE PRODUCT
    // ==========================================

    @Test
    void updateProduct_shouldUpdateProduct() {

        Product updatedProduct = new Product();
        updatedProduct.setName("New Laptop");
        updatedProduct.setPrice(85000);
        updatedProduct.setQuantity(15);
        updatedProduct.setDescription("Updated laptop");

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(productRepository.save(product))
                .thenReturn(product);

        Product result =
                productService.updateProduct(1L, updatedProduct);

        assertNotNull(result);
        assertEquals("New Laptop", result.getName());
        assertEquals(85000, result.getPrice());
        assertEquals(15, result.getQuantity());
        assertEquals("Updated laptop", result.getDescription());

        verify(productRepository).findById(1L);
        verify(productRepository).save(product);
    }

    @Test
    void updateProduct_shouldThrowWhenNotFound() {

        Product updatedProduct = new Product();
        updatedProduct.setName("New Laptop");
        updatedProduct.setPrice(85000);
        updatedProduct.setQuantity(15);

        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> productService.updateProduct(
                                999L,
                                updatedProduct
                        )
                );

        assertEquals(
                "Product not found: 999",
                exception.getMessage()
        );

        verify(productRepository, never()).save(any());
    }

    // ==========================================
    // SOFT DELETE PRODUCT
    // ==========================================

    @Test
    void deleteProduct_shouldMarkProductInactive() {

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        productService.deleteProduct(1L);

        assertFalse(product.isActive());

        verify(productRepository).findById(1L);
        verify(productRepository).save(product);
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void deleteProduct_shouldThrowWhenNotFound() {

        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.deleteProduct(999L)
        );

        verify(productRepository, never()).save(any());
        verify(productRepository, never()).delete(any(Product.class));
    }

    // ==========================================
    // REDUCE STOCK
    // ==========================================

    @Test
    void reduceQuantity_shouldReduceStock() {

        Product updatedProduct = new Product();
        updatedProduct.setId(1L);
        updatedProduct.setName("Laptop");
        updatedProduct.setPrice(75000);
        updatedProduct.setQuantity(7);
        updatedProduct.setActive(true);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product), Optional.of(updatedProduct));

        when(productRepository.reduceStockIfAvailable(1L, 3))
                .thenReturn(1);

        Product result =
                productService.reduceQuantity(1L, 3);

        assertNotNull(result);
        assertEquals(7, result.getQuantity());

        verify(productRepository)
                .reduceStockIfAvailable(1L, 3);

        verify(productRepository, times(2)).findById(1L);

        verify(productRepository, never()).save(any());
    }

    @Test
    void reduceQuantity_shouldRejectZeroQuantity() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> productService.reduceQuantity(1L, 0)
                );

        assertEquals(
                "Quantity must be greater than zero",
                exception.getMessage()
        );

        verifyNoInteractions(productRepository);
    }

    @Test
    void reduceQuantity_shouldRejectNegativeQuantity() {

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.reduceQuantity(1L, -1)
        );

        verifyNoInteractions(productRepository);
    }

    @Test
    void reduceQuantity_shouldRejectInsufficientStock() {

        // Product exists and is active.
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        // Atomic stock reduction fails.
        when(productRepository.reduceStockIfAvailable(1L, 20))
                .thenReturn(0);

        // The product still exists, so stock is insufficient.
        when(productRepository.existsById(1L))
                .thenReturn(true);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> productService.reduceQuantity(1L, 20)
                );

        assertEquals(
                "Insufficient stock for product: 1",
                exception.getMessage()
        );

        verify(productRepository).findById(1L);

        verify(productRepository)
                .reduceStockIfAvailable(1L, 20);

        verify(productRepository).existsById(1L);

        verify(productRepository, never()).save(any());
    }

    @Test
    void reduceQuantity_shouldThrowWhenProductNotFound() {

        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> productService.reduceQuantity(999L, 2)
                );

        assertEquals(
                "Product not found: 999",
                exception.getMessage()
        );

        verify(productRepository).findById(999L);

        verify(productRepository, never())
                .reduceStockIfAvailable(anyLong(), anyInt());
    }

    @Test
    void reduceQuantity_shouldRejectInactiveProduct() {

        product.setActive(false);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> productService.reduceQuantity(1L, 2)
                );

        assertEquals(
                "Product is no longer available: 1",
                exception.getMessage()
        );

        verify(productRepository, never())
                .reduceStockIfAvailable(anyLong(), anyInt());
    }

    // ==========================================
    // INCREASE STOCK
    // ==========================================

    @Test
    void increaseQuantity_shouldIncreaseStock() {

        when(productRepository.increaseStock(1L, 5))
                .thenReturn(1);

        Product updatedProduct = new Product();
        updatedProduct.setId(1L);
        updatedProduct.setName("Laptop");
        updatedProduct.setPrice(75000);
        updatedProduct.setQuantity(15);
        updatedProduct.setActive(true);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(updatedProduct));

        Product result =
                productService.increaseQuantity(1L, 5);

        assertNotNull(result);
        assertEquals(15, result.getQuantity());

        verify(productRepository).increaseStock(1L, 5);
        verify(productRepository).findById(1L);
    }

    @Test
    void increaseQuantity_shouldRejectInvalidQuantity() {

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.increaseQuantity(1L, 0)
        );

        verifyNoInteractions(productRepository);
    }

    @Test
    void increaseQuantity_shouldThrowWhenProductNotFound() {

        when(productRepository.increaseStock(999L, 5))
                .thenReturn(0);

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> productService.increaseQuantity(999L, 5)
                );

        assertEquals(
                "Product not found: 999",
                exception.getMessage()
        );

        verify(productRepository).increaseStock(999L, 5);
    }
}
