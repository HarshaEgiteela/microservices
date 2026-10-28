
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

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
        product.setCategory("Electronics");
        product.setImageUrl("https://example.com/laptop.jpg");
        product.setActive(true);
    }

    @Test
    void searchProducts_shouldReturnPaginatedProducts() {
        Page<Product> repositoryPage = new PageImpl<>(List.of(product));

        when(productRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(repositoryPage);

        Page<Product> result = productService.searchProducts(
                null, null, null, null, "name_asc", 0, 20
        );

        assertEquals(1, result.getTotalElements());
        assertEquals("Laptop", result.getContent().get(0).getName());

        verify(productRepository).findAll(
                any(Specification.class),
                argThat((Pageable pageable) ->
                        pageable.getPageNumber() == 0
                                && pageable.getPageSize() == 20
                                && pageable.getSort().getOrderFor("name") != null
                )
        );
    }

    @Test
    void searchProducts_shouldApplyFiltersAndSort() {
        when(productRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(product)));

        Page<Product> result = productService.searchProducts(
                "lap", "Electronics", 1000.0, 90000.0,
                "price_desc", 1, 5
        );

        assertEquals(1, result.getNumberOfElements());

        verify(productRepository).findAll(
                any(Specification.class),
                argThat((Pageable pageable) ->
                        pageable.getPageNumber() == 1
                                && pageable.getPageSize() == 5
                                && pageable.getSort().getOrderFor("price") != null
                                && pageable.getSort().getOrderFor("price").isDescending()
                )
        );
    }

    @Test
    void searchProducts_shouldRejectNegativePage() {
        assertThrows(IllegalArgumentException.class,
                () -> productService.searchProducts(
                        null, null, null, null, "name_asc", -1, 20
                ));

        verifyNoInteractions(productRepository);
    }

    @Test
    void searchProducts_shouldRejectInvalidPageSize() {
        assertThrows(IllegalArgumentException.class,
                () -> productService.searchProducts(
                        null, null, null, null, "name_asc", 0, 0
                ));

        assertThrows(IllegalArgumentException.class,
                () -> productService.searchProducts(
                        null, null, null, null, "name_asc", 0, 101
                ));

        verifyNoInteractions(productRepository);
    }

    @Test
    void searchProducts_shouldRejectInvalidPriceRange() {
        assertThrows(IllegalArgumentException.class,
                () -> productService.searchProducts(
                        null, null, 90000.0, 1000.0,
                        "name_asc", 0, 20
                ));

        verifyNoInteractions(productRepository);
    }

    @Test
    void getProductById_shouldReturnProductWhenFound() {
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        Product result = productService.getProductById(1L);

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

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> productService.getProductById(999L)
        );

        assertEquals("Product not found: 999", exception.getMessage());
    }

    @Test
    void createProduct_shouldSaveProduct() {
        Product saved = new Product();
        saved.setId(1L);
        saved.setName("Laptop");
        saved.setPrice(75000);
        saved.setQuantity(10);
        saved.setActive(true);
        saved.setCategory("Electronics");

        when(productRepository.save(any(Product.class)))
                .thenReturn(saved);

        Product result = productService.createProduct(product);

        assertEquals(1L, result.getId());
        assertEquals("Laptop", result.getName());
        assertTrue(result.isActive());
        assertNull(product.getId());

        verify(productRepository).save(product);
    }

    @Test
    void createProduct_shouldRejectBlankName() {
        product.setName("   ");

        assertThrows(IllegalArgumentException.class,
                () -> productService.createProduct(product));

        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_shouldRejectNegativePrice() {
        product.setPrice(-100);

        assertThrows(IllegalArgumentException.class,
                () -> productService.createProduct(product));

        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_shouldRejectNegativeQuantity() {
        product.setQuantity(-5);

        assertThrows(IllegalArgumentException.class,
                () -> productService.createProduct(product));

        verify(productRepository, never()).save(any());
    }

    @Test
    void updateProduct_shouldUpdateProduct() {
        Product updated = new Product();
        updated.setName("New Laptop");
        updated.setPrice(85000);
        updated.setQuantity(15);
        updated.setDescription("Updated laptop");
        updated.setCategory("Electronics");
        updated.setImageUrl("https://example.com/new-laptop.jpg");

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));
        when(productRepository.save(product))
                .thenReturn(product);

        Product result = productService.updateProduct(1L, updated);

        assertEquals("New Laptop", result.getName());
        assertEquals(85000, result.getPrice());
        assertEquals(15, result.getQuantity());
        assertEquals("Updated laptop", result.getDescription());
        assertEquals("Electronics", result.getCategory());
        assertEquals(
                "https://example.com/new-laptop.jpg",
                result.getImageUrl()
        );

        verify(productRepository).findById(1L);
        verify(productRepository).save(product);
    }

    @Test
    void updateProduct_shouldThrowWhenNotFound() {
        Product updated = new Product();
        updated.setName("New Laptop");
        updated.setPrice(85000);
        updated.setQuantity(15);

        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.updateProduct(999L, updated));

        verify(productRepository, never()).save(any());
    }

    @Test
    void deleteProduct_shouldMarkProductInactive() {
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        productService.deleteProduct(1L);

        assertFalse(product.isActive());
        verify(productRepository).save(product);
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void deleteProduct_shouldThrowWhenNotFound() {
        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.deleteProduct(999L));

        verify(productRepository, never()).save(any());
    }

    @Test
    void reduceQuantity_shouldReduceStock() {
        Product updated = new Product();
        updated.setId(1L);
        updated.setName("Laptop");
        updated.setPrice(75000);
        updated.setQuantity(7);
        updated.setActive(true);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product), Optional.of(updated));
        when(productRepository.reduceStockIfAvailable(1L, 3))
                .thenReturn(1);

        Product result = productService.reduceQuantity(1L, 3);

        assertEquals(7, result.getQuantity());
        verify(productRepository).reduceStockIfAvailable(1L, 3);
        verify(productRepository, times(2)).findById(1L);
        verify(productRepository, never()).save(any());
    }

    @Test
    void reduceQuantity_shouldRejectZeroQuantity() {
        IllegalArgumentException exception = assertThrows(
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
        assertThrows(IllegalArgumentException.class,
                () -> productService.reduceQuantity(1L, -1));

        verifyNoInteractions(productRepository);
    }

    @Test
    void reduceQuantity_shouldRejectInsufficientStock() {
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));
        when(productRepository.reduceStockIfAvailable(1L, 20))
                .thenReturn(0);
        when(productRepository.existsById(1L))
                .thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> productService.reduceQuantity(1L, 20)
        );

        assertEquals(
                "Insufficient stock for product: 1",
                exception.getMessage()
        );

        verify(productRepository).reduceStockIfAvailable(1L, 20);
        verify(productRepository).existsById(1L);
    }

    @Test
    void reduceQuantity_shouldThrowWhenProductNotFound() {
        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.reduceQuantity(999L, 2));

        verify(productRepository, never())
                .reduceStockIfAvailable(anyLong(), anyInt());
    }

    @Test
    void reduceQuantity_shouldRejectInactiveProduct() {
        product.setActive(false);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        IllegalArgumentException exception = assertThrows(
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

    @Test
    void increaseQuantity_shouldIncreaseStock() {
        Product updated = new Product();
        updated.setId(1L);
        updated.setName("Laptop");
        updated.setPrice(75000);
        updated.setQuantity(15);
        updated.setActive(true);

        when(productRepository.increaseStock(1L, 5))
                .thenReturn(1);
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(updated));

        Product result = productService.increaseQuantity(1L, 5);

        assertEquals(15, result.getQuantity());
        verify(productRepository).increaseStock(1L, 5);
        verify(productRepository).findById(1L);
    }

    @Test
    void increaseQuantity_shouldRejectInvalidQuantity() {
        assertThrows(IllegalArgumentException.class,
                () -> productService.increaseQuantity(1L, 0));

        verifyNoInteractions(productRepository);
    }

    @Test
    void increaseQuantity_shouldThrowWhenProductNotFound() {
        when(productRepository.increaseStock(999L, 5))
                .thenReturn(0);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> productService.increaseQuantity(999L, 5)
        );

        assertEquals("Product not found: 999", exception.getMessage());
        verify(productRepository).increaseStock(999L, 5);
    }
}
