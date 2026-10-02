package com.example.order.presentation.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        String productId,
        int quantity,
        BigDecimal price,
        String productName,
        String imageUrl
) {}