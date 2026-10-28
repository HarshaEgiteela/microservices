
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

    private Product product(Long id, String name, double price, int quantity) {
        return new Product(
                id, name, price, quantity,
                "Test description", true,
                "Electronics", "https://example.com/product.jpg"
        );
    }

    @Test
    void getProducts_shouldReturnPaginatedProducts() throws Exception {
        Product laptop = product(1L, "Laptop", 75000, 10);
        Product phone = product(2L, "Phone", 30000, 20);

        Page<Product> page = new PageImpl<>(
                List.of(laptop, phone),
                PageRequest.of(0, 20),
                2
        );

        when(productService.searchProducts(
                isNull(), isNull(), isNull(), isNull(),
                eq("name_asc"), eq(0), eq(20)
        )).thenReturn(page);

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Laptop"))
                .andExpect(jsonPath("$.content[0].category").value("Electronics"))
                .andExpect(jsonPath("$.content[1].name").value("Phone"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.number").value(0));

        verify(productService).searchProducts(
                null, null, null, null, "name_asc", 0, 20
        );
    }

    @Test
    void getProducts_shouldPassFiltersAndPagination() throws Exception {
        Product laptop = product(1L, "Laptop", 75000, 10);

        Page<Product> page = new PageImpl<>(
                List.of(laptop),
                PageRequest.of(1, 5),
                6
        );

        when(productService.searchProducts(
                eq("lap"), eq("Electronics"),
                eq(1000.0), eq(90000.0),
                eq("price_desc"), eq(1), eq(5)
        )).thenReturn(page);

        mockMvc.perform(get("/products")
                        .param("search", "lap")
                        .param("category", "Electronics")
                        .param("minPrice", "1000")
                        .param("maxPrice", "90000")
                        .param("sort", "price_desc")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Laptop"))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(6));

        verify(productService).searchProducts(
                "lap", "Electronics",
                1000.0, 90000.0, "price_desc", 1, 5
        );
    }

    @Test
    void getProduct_shouldReturnProduct() throws Exception {
        when(productService.getProductById(1L))
                .thenReturn(product(1L, "Laptop", 75000, 10));

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.price").value(75000))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.category").value("Electronics"))
                .andExpect(jsonPath("$.imageUrl")
                        .value("https://example.com/product.jpg"));

        verify(productService).getProductById(1L);
    }

    @Test
    void getProduct_shouldReturn404WhenProductNotFound() throws Exception {
        when(productService.getProductById(999L))
                .thenThrow(new ResourceNotFoundException("Product not found: 999"));

        mockMvc.perform(get("/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Product not found: 999"));

        verify(productService).getProductById(999L);
    }

    @Test
    void createProduct_shouldReturnCreatedProduct() throws Exception {
        Product input = product(null, "Laptop", 75000, 10);
        Product saved = product(1L, "Laptop", 75000, 10);

        when(productService.createProduct(any(Product.class)))
                .thenReturn(saved);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.category").value("Electronics"))
                .andExpect(jsonPath("$.quantity").value(10));

        verify(productService).createProduct(any(Product.class));
    }

    @Test
    void updateProduct_shouldReturnUpdatedProduct() throws Exception {
        Product input = product(null, "Updated Laptop", 80000, 15);
        Product saved = product(1L, "Updated Laptop", 80000, 15);

        when(productService.updateProduct(eq(1L), any(Product.class)))
                .thenReturn(saved);

        mockMvc.perform(put("/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Updated Laptop"))
                .andExpect(jsonPath("$.price").value(80000))
                .andExpect(jsonPath("$.quantity").value(15))
                .andExpect(jsonPath("$.category").value("Electronics"));

        verify(productService).updateProduct(eq(1L), any(Product.class));
    }

    @Test
    void deleteProduct_shouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/products/1"))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(1L);
    }

    @Test
    void reduceStock_shouldReturnUpdatedProduct() throws Exception {
        when(productService.reduceQuantity(1L, 3))
                .thenReturn(product(1L, "Laptop", 75000, 7));

        mockMvc.perform(put("/products/1/reduce-stock")
                        .param("quantity", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.quantity").value(7));

        verify(productService).reduceQuantity(1L, 3);
    }
}
