package com.example.order.presentation.dto;

import com.example.order.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String username,
        OrderStatus status,
        List<OrderItemResponse> items,
        BigDecimal total,
        List<StatusHistoryEntryResponse> history,
        Instant createdAt
) {}
