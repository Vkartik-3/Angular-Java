package com.example.order.presentation.mapper;

import com.example.order.domain.model.Order;
import com.example.order.presentation.dto.OrderItemResponse;
import com.example.order.presentation.dto.OrderResponse;
import com.example.order.presentation.dto.StatusHistoryEntryResponse;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.util.List;

@ApplicationScoped
public class OrderPresentationMapper {

    public OrderResponse toResponse(Order order) {
        return toResponse(order, List.of());
    }

    public OrderResponse toResponse(Order order, List<StatusHistoryEntryResponse> history) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(i -> new OrderItemResponse(
                        i.productId(),
                        i.quantity(),
                        i.price().amount(),
                        i.productName(),
                        i.imageUrl()
                ))
                .toList();

        BigDecimal total = order.getItems().stream()
                .map(i -> i.price().amount().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new OrderResponse(
                order.getId().value(),
                order.getUsername(),
                order.getStatus(),
                items,
                total,
                history,
                order.getCreatedAt()
        );
    }
}