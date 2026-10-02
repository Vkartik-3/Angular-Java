package com.example.product.resource;

import com.example.product.domain.Product;
import com.example.product.dto.ImportResult;
import com.example.product.service.ProductImportService;
import com.example.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProductResourceTest {

    private MockMvc mockMvc;
    private final ProductService productService = mock(ProductService.class);
    private final ProductImportService importService = mock(ProductImportService.class);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductResource(productService, importService)).build();
    }

    @Test
    void getAll_returns200WithProductList() throws Exception {
        when(productService.findAll()).thenReturn(List.of(
                Product.builder()
                        .id("507f1f77bcf86cd799439011")
                        .category("vinyl")
                        .attributes(Map.of("artist", "Pink Floyd", "title", "The Wall"))
                        .price(new BigDecimal("29.99"))
                        .createdAt(Instant.now())
                        .build()
        ));
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].attributes.artist").value("Pink Floyd"));
    }

    @Test
    void importProducts_validCsv_returns200WithImportResult() throws Exception {
        when(importService.importCsv(any())).thenReturn(
                ImportResult.builder().imported(3).skipped(0).skippedRecords(List.of()).build()
        );
        MockMultipartFile file = new MockMultipartFile("file", "products.csv", "text/csv",
                "category,artist,title,price,imageUrl\n".getBytes());
        mockMvc.perform(multipart("/products/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(3))
                .andExpect(jsonPath("$.skipped").value(0));
    }

    @Test
    void importProducts_missingFile_returns400() throws Exception {
        mockMvc.perform(multipart("/products/import"))
                .andExpect(status().isBadRequest());
    }
}
