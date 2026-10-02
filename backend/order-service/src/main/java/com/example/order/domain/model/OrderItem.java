package com.example.order.domain.model;

import com.example.order.domain.valueobject.Money;
import com.example.order.domain.valueobject.OrderId;

import java.util.UUID;

public record OrderItem(
        UUID id,
        OrderId orderId,
        String productId,
        int quantity,
        Money price,
        String productName,
        String imageUrl
) {}