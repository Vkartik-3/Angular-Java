package com.example.order.application.saga;

import com.example.order.application.port.input.OrderUseCase;
import com.example.order.domain.event.OrderSagaCompletedEvent;
import com.example.order.domain.event.OrderStatusChangedEvent;
import com.example.order.domain.event.PaymentRollbackEvent;
import com.example.order.domain.model.HistoryStatus;
import com.example.order.domain.model.Order;
import com.example.order.domain.valueobject.OrderId;
import com.example.order.infrastructure.observability.OrderMetrics;
import com.example.order.infrastructure.outbox.OutboxEventType;
import com.example.order.infrastructure.outbox.OutboxService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;

@ApplicationScoped
public class SagaTimeoutJob {

    @ConfigProperty(name = "app.saga.step-timeout-seconds", defaultValue = "30")
    long stepTimeoutSeconds;

    @Inject OrderSagaRepository sagaRepository;
    @Inject OrderUseCase orderService;
    @Inject OutboxService outbox;
    @Inject Event<OrderStatusChangedEvent> statusChangedEvent;
    @Inject Event<OrderSagaCompletedEvent> sagaCompletedEvent;
    @Inject OrderMetrics metrics;

    @Scheduled(every = "10s")
    @Transactional
    void checkTimeouts() {
        var expired = sagaRepository.findExpired(Instant.now());
        for (OrderSagaState saga : expired) {
            handleTimeout(saga);
        }
    }

    private void handleTimeout(OrderSagaState saga) {
        var orderId = new OrderId(saga.getOrderId());

        switch (saga.getStep()) {
            case WAITING_PAYMENT, WAITING_ROLLBACK -> {
                orderService.cancel(orderId);
                statusChangedEvent.fire(new OrderStatusChangedEvent(saga.getOrderId(), HistoryStatus.CANCELLED, true));
                saga.setStep(OrderSagaState.SagaStep.CANCELLED);
                sagaCompletedEvent.fire(new OrderSagaCompletedEvent(saga.getOrderId()));
                metrics.sagaCancelled(saga.getOrderId());
                metrics.sagaTimedOut();
            }
            case WAITING_INVENTORY -> {
                orderService.cancel(orderId);
                statusChangedEvent.fire(new OrderStatusChangedEvent(saga.getOrderId(), HistoryStatus.CANCELLED));
                outbox.save(
                        Order.class.getSimpleName(),
                        saga.getOrderId().toString(),
                        OutboxEventType.PAYMENT_ROLLBACK,
                        PaymentRollbackEvent.of(saga.getOrderId(), saga.getCorrelationId())
                );
                saga.setStep(OrderSagaState.SagaStep.WAITING_ROLLBACK);
                saga.setDeadline(Instant.now().plusSeconds(stepTimeoutSeconds));
                metrics.sagaTimedOut();
                // No completion signal — onPaymentRolledBack() will fire sagaCancelled + sagaCompensated
            }
        }
    }
}