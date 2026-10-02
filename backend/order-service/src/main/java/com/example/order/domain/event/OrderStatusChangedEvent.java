package com.example.order.domain.event;

import com.example.order.domain.model.HistoryStatus;

import java.util.UUID;

/**
 * sagaCompleted is true exactly when this status change is immediately followed by an
 * OrderSagaCompletedEvent.fire() for the same order in the same transaction.
 * Carried here rather than as a separate CDI event because
 * {@code @Observes(during = AFTER_SUCCESS)} does not guarantee cross-event-type
 * delivery order — in testing the completion signal sometimes arrived before
 * the final status, silently dropping it from the SSE timeline. See OrderSseService.
 */
public record OrderStatusChangedEvent(UUID orderId, HistoryStatus status, boolean sagaCompleted) {

    public OrderStatusChangedEvent(UUID orderId, HistoryStatus status) {
        this(orderId, status, false);
    }
}