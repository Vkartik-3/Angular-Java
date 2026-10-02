package com.example.order.application.port.output;

import com.example.order.domain.model.HistoryStatus;

import java.util.UUID;

public interface OrderHistoryRecorder {
    void record(UUID orderId, HistoryStatus status);
}