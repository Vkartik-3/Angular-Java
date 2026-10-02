package com.example.order.infrastructure.history;

import com.example.order.application.port.output.OrderHistoryRecorder;
import com.example.order.domain.model.HistoryStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class OrderStatusHistoryService implements OrderHistoryRecorder {

    @Inject OrderStatusHistoryRepository repository;

    public void record(UUID orderId, HistoryStatus status) {
        repository.persist(OrderStatusHistory.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .status(status)
                .occurredAt(Instant.now())
                .build());
    }

    public List<OrderStatusHistory> findByOrderId(UUID orderId) {
        return repository.findByOrderId(orderId);
    }
}