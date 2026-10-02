package com.example.order.application.saga;

import com.example.order.application.port.input.OrderUseCase;
import com.example.order.application.port.output.OrderRepository;
import com.example.order.domain.event.InventoryApprovedEvent;
import com.example.order.domain.event.InventoryRejectedEvent;
import com.example.order.domain.event.OrderSagaCompletedEvent;
import com.example.order.domain.event.OrderStatusChangedEvent;
import com.example.order.domain.event.PaymentCompletedEvent;
import com.example.order.domain.event.PaymentFailedEvent;
import com.example.order.domain.valueobject.OrderId;
import com.example.order.application.port.output.OrderHistoryRecorder;
import com.example.order.infrastructure.inbox.InboxEventType;
import com.example.order.infrastructure.inbox.InboxService;
import com.example.order.infrastructure.observability.CorrelationIdProvider;
import com.example.order.infrastructure.observability.OrderMetrics;
import com.example.order.infrastructure.outbox.OutboxEventType;
import com.example.order.infrastructure.outbox.OutboxService;
import jakarta.enterprise.event.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unused")
class OrderSagaOrchestratorTest {

    @Mock OutboxService outbox;
    @Mock OrderUseCase service;
    @Mock OrderRepository orderRepository;
    @Mock CorrelationIdProvider correlationIdProvider;
    @Mock OrderSagaRepository sagaRepository;
    @Mock InboxService inbox;
    @Mock OrderHistoryRecorder historyService;
    @Mock Event<OrderStatusChangedEvent> statusChangedEvent;
    @Mock Event<OrderSagaCompletedEvent> sagaCompletedEvent;
    @Mock OrderMetrics metrics;

    @InjectMocks
    OrderSagaOrchestrator orchestrator;

    @Test
    void onPaymentCompleted_advancesSagaToWaitingInventoryAndPublishesRequest() {
        UUID orderId = UUID.randomUUID();
        PaymentCompletedEvent event = PaymentCompletedEvent.of(orderId, "corr-1");
        OrderSagaState saga = sagaInStep(orderId, OrderSagaState.SagaStep.WAITING_PAYMENT);

        when(inbox.receive(event.eventId(), InboxEventType.PAYMENT_COMPLETED)).thenReturn(true);
        when(sagaRepository.findById(orderId)).thenReturn(saga);

        orchestrator.onPaymentCompleted(event);

        verify(service).pay(any(OrderId.class));
        verify(outbox).save(any(), any(), eq(OutboxEventType.INVENTORY_REQUEST), any());
        assertEquals(OrderSagaState.SagaStep.WAITING_INVENTORY, saga.getStep());
    }

    @Test
    void onPaymentCompleted_duplicateEvent_skipsProcessing() {
        UUID orderId = UUID.randomUUID();
        PaymentCompletedEvent event = PaymentCompletedEvent.of(orderId, "corr-1");

        when(inbox.receive(event.eventId(), InboxEventType.PAYMENT_COMPLETED)).thenReturn(false);

        orchestrator.onPaymentCompleted(event);

        verify(service, never()).pay(any());
        verify(outbox, never()).save(any(), any(), any(), any());
    }

    @Test
    void onPaymentCompleted_alreadyCancelledSaga_skipsProcessing() {
        UUID orderId = UUID.randomUUID();
        PaymentCompletedEvent event = PaymentCompletedEvent.of(orderId, "corr-1");

        when(inbox.receive(event.eventId(), InboxEventType.PAYMENT_COMPLETED)).thenReturn(true);
        when(sagaRepository.findById(orderId)).thenReturn(sagaInStep(orderId, OrderSagaState.SagaStep.CANCELLED));

        orchestrator.onPaymentCompleted(event);

        verify(service, never()).pay(any());
    }

    @Test
    void onPaymentFailed_cancelsSagaAndFiresCompletedEvent() {
        UUID orderId = UUID.randomUUID();
        PaymentFailedEvent event = PaymentFailedEvent.of(orderId, "Insufficient funds", "corr-1");
        OrderSagaState saga = sagaInStep(orderId, OrderSagaState.SagaStep.WAITING_PAYMENT);

        when(inbox.receive(event.eventId(), InboxEventType.PAYMENT_FAILED)).thenReturn(true);
        when(sagaRepository.findById(orderId)).thenReturn(saga);

        orchestrator.onPaymentFailed(event);

        verify(service).failPayment(any(OrderId.class));
        verify(sagaCompletedEvent).fire(any());
        assertEquals(OrderSagaState.SagaStep.CANCELLED, saga.getStep());
    }

    @Test
    void onInventoryApproved_completesSaga() {
        UUID orderId = UUID.randomUUID();
        InventoryApprovedEvent event = InventoryApprovedEvent.of(orderId, "corr-1");
        OrderSagaState saga = sagaInStep(orderId, OrderSagaState.SagaStep.WAITING_INVENTORY);

        when(inbox.receive(event.eventId(), InboxEventType.INVENTORY_APPROVED)).thenReturn(true);
        when(sagaRepository.findById(orderId)).thenReturn(saga);

        orchestrator.onInventoryApproved(event);

        verify(service).approveInventory(any(OrderId.class));
        verify(sagaCompletedEvent).fire(any());
        assertEquals(OrderSagaState.SagaStep.COMPLETED, saga.getStep());
    }

    @Test
    void onInventoryRejected_cancelsOrderAndPublishesPaymentRollback() {
        UUID orderId = UUID.randomUUID();
        InventoryRejectedEvent event = InventoryRejectedEvent.of(orderId, "Out of stock", "corr-1");
        OrderSagaState saga = sagaInStep(orderId, OrderSagaState.SagaStep.WAITING_INVENTORY);

        when(inbox.receive(event.eventId(), InboxEventType.INVENTORY_REJECTED)).thenReturn(true);
        when(sagaRepository.findById(orderId)).thenReturn(saga);

        orchestrator.onInventoryRejected(event);

        verify(service).rejectInventory(any(OrderId.class));
        verify(outbox).save(any(), any(), eq(OutboxEventType.PAYMENT_ROLLBACK), any());
        assertEquals(OrderSagaState.SagaStep.WAITING_ROLLBACK, saga.getStep());
    }

    private OrderSagaState sagaInStep(UUID orderId, OrderSagaState.SagaStep step) {
        return OrderSagaState.builder()
                .orderId(orderId)
                .step(step)
                .deadline(Instant.now().plusSeconds(30))
                .correlationId("corr-1")
                .build();
    }
}
