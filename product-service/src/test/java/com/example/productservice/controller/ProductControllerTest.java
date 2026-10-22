
package com.example.productservice.controller;

import com.example.productservice.entity.Product;
import com.example.productservice.exception.GlobalExceptionHandler;
import com.example.productservice.exception.ResourceNotFoundException;
import com.example.productservice.service.ProductService;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {

        mockMvc = MockMvcBuilders
                .standaloneSetup(productController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
    }

    // ==========================================
    // GET ALL PRODUCTS
    // ==========================================

    @Test
    void getAllProducts_shouldReturnProducts() throws Exception {

        Product product1 = new Product(
                1L, "Laptop", 75000, 10, "Test laptop", true
        );

        Product product2 = new Product(
                2L, "Phone", 30000, 20, "Test phone", true
        );

        when(productService.getAllProducts())
                .thenReturn(List.of(product1, product2));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Laptop"))
                .andExpect(jsonPath("$[0].price").value(75000))
                .andExpect(jsonPath("$[1].name").value("Phone"));

        verify(productService).getAllProducts();
    }

    // ==========================================
    // GET PRODUCT BY ID
    // ==========================================

    @Test
    void getProduct_shouldReturnProduct() throws Exception {

        Product product = new Product(
                1L, "Laptop", 75000, 10, "Test laptop", true
        );

        when(productService.getProductById(1L))
                .thenReturn(product);

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.price").value(75000))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.description").value("Test laptop"));

        verify(productService).getProductById(1L);
    }

    // ==========================================
    // PRODUCT NOT FOUND
    // ==========================================

    @Test
    void getProduct_shouldReturn404WhenProductNotFound()
            throws Exception {

        when(productService.getProductById(999L))
                .thenThrow(
                        new ResourceNotFoundException(
                                "Product not found: 999"
                        )
                );

        mockMvc.perform(get("/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Product not found: 999"));

        verify(productService).getProductById(999L);
    }

    // ==========================================
    // CREATE PRODUCT
    // ==========================================

    @Test
    void createProduct_shouldReturnCreatedProduct()
            throws Exception {

        Product product = new Product(
                null, "Laptop", 75000, 10, "Test laptop", true
        );

        Product savedProduct = new Product(
                1L, "Laptop", 75000, 10, "Test laptop", true
        );

        when(productService.createProduct(any(Product.class)))
                .thenReturn(savedProduct);

        mockMvc.perform(
                        post("/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(product)
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.price").value(75000))
                .andExpect(jsonPath("$.quantity").value(10));

        verify(productService)
                .createProduct(any(Product.class));
    }

    // ==========================================
    // UPDATE PRODUCT
    // ==========================================

    @Test
    void updateProduct_shouldReturnUpdatedProduct()
            throws Exception {

        Product updatedProduct = new Product(
                null,
                "Updated Laptop",
                80000,
                15,
                "Updated description",
                true
        );

        Product resultProduct = new Product(
                1L,
                "Updated Laptop",
                80000,
                15,
                "Updated description",
                true
        );

        when(productService.updateProduct(
                eq(1L),
                any(Product.class)
        )).thenReturn(resultProduct);

        mockMvc.perform(
                        put("/products/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                updatedProduct
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name")
                        .value("Updated Laptop"))
                .andExpect(jsonPath("$.price").value(80000))
                .andExpect(jsonPath("$.quantity").value(15))
                .andExpect(jsonPath("$.description")
                        .value("Updated description"));

        verify(productService)
                .updateProduct(
                        eq(1L),
                        any(Product.class)
                );
    }

    // ==========================================
    // DELETE PRODUCT
    // ==========================================

    @Test
    void deleteProduct_shouldReturnNoContent()
            throws Exception {

        mockMvc.perform(delete("/products/1"))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(1L);
    }

    // ==========================================
    // REDUCE STOCK
    // ==========================================

    @Test
    void reduceStock_shouldReturnUpdatedProduct()
            throws Exception {

        Product product = new Product(
                1L, "Laptop", 75000, 7, "Test laptop", true
        );

        when(productService.reduceQuantity(1L, 3))
                .thenReturn(product);

        mockMvc.perform(
                        put("/products/1/reduce-stock")
                                .param("quantity", "3")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.quantity").value(7));

        verify(productService).reduceQuantity(1L, 3);
    }
}
