package com.example.product.dto;

import lombok.Data;

import java.util.Map;

@Data
public class ProductCsv {
    private String category;
    private String price;
    private String imageUrl;
    private Map<String, String> attributes;
    private int lineNumber;
}