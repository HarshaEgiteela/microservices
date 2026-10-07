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


    // =========================================================
    // GET /products
    // =========================================================

    @Test
    void getAllProducts_shouldReturnProducts() throws Exception {

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


        when(productService.getAllProducts())
                .thenReturn(List.of(product1, product2));


        mockMvc
                .perform(get("/products"))

                .andExpect(status().isOk())

                .andExpect(jsonPath("$").isArray())

                .andExpect(jsonPath("$.length()")
                        .value(2))

                .andExpect(jsonPath("$[0].id")
                        .value(1))

                .andExpect(jsonPath("$[0].name")
                        .value("Laptop"))

                .andExpect(jsonPath("$[0].price")
                        .value(75000))

                .andExpect(jsonPath("$[1].name")
                        .value("Phone"));


        verify(productService)
                .getAllProducts();
    }


    // =========================================================
    // GET /products/{id}
    // =========================================================

    @Test
    void getProduct_shouldReturnProduct() throws Exception {

        Product product = new Product();

        product.setId(1L);
        product.setName("Laptop");
        product.setPrice(75000);
        product.setQuantity(10);
        product.setDescription("Test laptop");


        when(productService.getProductById(1L))
                .thenReturn(product);


        mockMvc
                .perform(get("/products/1"))

                .andExpect(status().isOk())

                .andExpect(jsonPath("$.id")
                        .value(1))

                .andExpect(jsonPath("$.name")
                        .value("Laptop"))

                .andExpect(jsonPath("$.price")
                        .value(75000))

                .andExpect(jsonPath("$.quantity")
                        .value(10))

                .andExpect(jsonPath("$.description")
                        .value("Test laptop"));


        verify(productService)
                .getProductById(1L);
    }


    // =========================================================
    // GET /products/{id} - NOT FOUND
    // =========================================================

    @Test
    void getProduct_shouldReturn404WhenProductNotFound()
            throws Exception {

        when(productService.getProductById(999L))
                .thenThrow(
                        new ResourceNotFoundException(
                                "Product not found"
                        )
                );


        mockMvc
                .perform(get("/products/999"))

                .andExpect(status().isNotFound())

                .andExpect(content()
                        .string("Product not found"));


        verify(productService)
                .getProductById(999L);
    }


    // =========================================================
    // POST /products
    // =========================================================

    @Test
    void createProduct_shouldReturnCreatedProduct()
            throws Exception {

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


        when(productService.createProduct(any(Product.class)))
                .thenReturn(savedProduct);


        mockMvc
                .perform(
                        post("/products")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                product
                                        )
                                )
                )

                .andExpect(status().isOk())

                .andExpect(jsonPath("$.id")
                        .value(1))

                .andExpect(jsonPath("$.name")
                        .value("Laptop"))

                .andExpect(jsonPath("$.price")
                        .value(75000))

                .andExpect(jsonPath("$.quantity")
                        .value(10));


        verify(productService)
                .createProduct(any(Product.class));
    }


    // =========================================================
    // PUT /products/{id}
    // =========================================================

    @Test
    void updateProduct_shouldReturnUpdatedProduct()
            throws Exception {

        Product updatedProduct = new Product();

        updatedProduct.setName("Updated Laptop");
        updatedProduct.setPrice(80000);
        updatedProduct.setQuantity(15);
        updatedProduct.setDescription(
                "Updated description"
        );


        Product resultProduct = new Product();

        resultProduct.setId(1L);
        resultProduct.setName("Updated Laptop");
        resultProduct.setPrice(80000);
        resultProduct.setQuantity(15);
        resultProduct.setDescription(
                "Updated description"
        );


        when(productService.updateProduct(
                eq(1L),
                any(Product.class)
        )).thenReturn(resultProduct);


        mockMvc
                .perform(
                        put("/products/1")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                updatedProduct
                                        )
                                )
                )

                .andExpect(status().isOk())

                .andExpect(jsonPath("$.id")
                        .value(1))

                .andExpect(jsonPath("$.name")
                        .value("Updated Laptop"))

                .andExpect(jsonPath("$.price")
                        .value(80000))

                .andExpect(jsonPath("$.quantity")
                        .value(15))

                .andExpect(jsonPath("$.description")
                        .value("Updated description"));


        verify(productService)
                .updateProduct(
                        eq(1L),
                        any(Product.class)
                );
    }

    // =========================================================
    // DELETE /products/{id}
    // =========================================================

    @Test
    void deleteProduct_shouldReturnNoContent()
            throws Exception {

        doNothing()
                .when(productService)
                .deleteProduct(1L);


        mockMvc
                .perform(delete("/products/1"))

                .andExpect(status().isNoContent());


        verify(productService)
                .deleteProduct(1L);
    }


    // =========================================================
    // PUT /products/{id}/reduce-stock
    // =========================================================

    @Test
    void reduceStock_shouldReturnUpdatedProduct()
            throws Exception {

        Product product = new Product();

        product.setId(1L);
        product.setName("Laptop");
        product.setPrice(75000);
        product.setQuantity(7);
        product.setDescription("Test laptop");


        when(productService.reduceQuantity(1L, 3))
                .thenReturn(product);


        mockMvc
                .perform(
                        put("/products/1/reduce-stock")
                                .param("quantity", "3")
                )

                .andExpect(status().isOk())

                .andExpect(jsonPath("$.id")
                        .value(1))

                .andExpect(jsonPath("$.name")
                        .value("Laptop"))

                .andExpect(jsonPath("$.quantity")
                        .value(7));


        verify(productService)
                .reduceQuantity(1L, 3);
    }
}