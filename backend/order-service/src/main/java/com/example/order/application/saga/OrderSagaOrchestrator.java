package com.example.order.application.saga;

import com.example.order.application.port.input.OrderUseCase;
import com.example.order.application.port.output.OrderRepository;
import com.example.order.domain.event.InventoryApprovedEvent;
import com.example.order.domain.event.InventoryRejectedEvent;
import com.example.order.domain.event.InventoryRequestEvent;
import com.example.order.domain.event.OrderSagaCompletedEvent;
import com.example.order.domain.event.OrderStatusChangedEvent;
import com.example.order.domain.event.PaymentCompletedEvent;
import com.example.order.domain.event.PaymentFailedEvent;
import com.example.order.domain.event.PaymentRequestEvent;
import com.example.order.domain.event.PaymentRollbackCompletedEvent;
import com.example.order.domain.event.PaymentRollbackEvent;
import com.example.order.domain.model.HistoryStatus;
import com.example.order.domain.model.Order;
import com.example.order.domain.valueobject.OrderId;
import com.example.order.application.port.output.OrderHistoryRecorder;
import com.example.order.infrastructure.inbox.InboxEventType;
import com.example.order.infrastructure.inbox.InboxService;
import com.example.order.infrastructure.observability.CorrelationIdProvider;
import com.example.order.infrastructure.observability.OrderMetrics;
import com.example.order.infrastructure.outbox.OutboxEventType;
import com.example.order.infrastructure.outbox.OutboxService;
import com.example.order.presentation.dto.CreateOrderRequest;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.MDC;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class OrderSagaOrchestrator {

    @ConfigProperty(name = "app.saga.step-timeout-seconds", defaultValue = "30")
    long stepTimeoutSeconds;

    @Inject OutboxService outbox;
    @Inject OrderUseCase service;
    @Inject OrderRepository orderRepository;
    @Inject CorrelationIdProvider correlationIdProvider;
    @Inject OrderSagaRepository sagaRepository;
    @Inject InboxService inbox;
    @Inject OrderHistoryRecorder historyService;
    @Inject Event<OrderStatusChangedEvent> statusChangedEvent;
    @Inject Event<OrderSagaCompletedEvent> sagaCompletedEvent;
    @Inject OrderMetrics metrics;

    @Transactional
    public UUID start(CreateOrderRequest request, UUID customerId, String username, UUID idempotencyKey) {
        if (idempotencyKey != null) {
            Optional<Order> existing = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                Log.infof("Duplicate request detected idempotencyKey=%s orderId=%s", idempotencyKey, existing.get().getId().value());
                return existing.get().getId().value();
            }
        }

        try {
            String correlationId = getCorrelationId();

            Order order = new Order(new OrderId(UUID.randomUUID()));
            order.setIdempotencyKey(idempotencyKey);
            order.setUserId(customerId);
            order.setUsername(username);
            request.items().forEach(i -> order.addItem(i.productId(), i.quantity(), i.price(), i.productName(), i.imageUrl()));
            var total = order.totalAmount();

            service.create(order);

            sagaRepository.persist(
                    OrderSagaState.builder()
                            .orderId(order.getId().value())
                            .step(OrderSagaState.SagaStep.WAITING_PAYMENT)
                            .deadline(Instant.now().plusSeconds(stepTimeoutSeconds))
                            .correlationId(correlationId)
                            .build()
            );

            outbox.save(
                    Order.class.getSimpleName(),
                    order.getId().value().toString(),
                    OutboxEventType.PAYMENT_REQUEST,
                    PaymentRequestEvent.of(order.getId().value(), customerId, total.amount(), correlationId)
            );

            MDC.put("orderId", order.getId().value().toString());
            metrics.orderCreated(order.getId().value());
            Log.infof("Saga started step=WAITING_PAYMENT total=%s", total.amount());
            return order.getId().value();

        } catch (PersistenceException e) {
            if (isUniqueConstraintViolation(e) && idempotencyKey != null) {
                Optional<Order> existing = orderRepository.findByIdempotencyKey(idempotencyKey);
                if (existing.isPresent()) {
                    Log.infof("Race condition resolved idempotencyKey=%s orderId=%s", idempotencyKey, existing.get().getId().value());
                    return existing.get().getId().value();
                }
            }
            throw e;
        }
    }

    @Transactional
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        if (!inbox.receive(event.eventId(), InboxEventType.PAYMENT_COMPLETED)) return;

        OrderSagaState saga = sagaRepository.findById(event.orderId());
        if (saga == null || saga.getStep() == OrderSagaState.SagaStep.CANCELLED) return;

        service.pay(new OrderId(event.orderId()));
        statusChangedEvent.fire(new OrderStatusChangedEvent(event.orderId(), HistoryStatus.PAID));

        saga.setStep(OrderSagaState.SagaStep.WAITING_INVENTORY);
        saga.setDeadline(Instant.now().plusSeconds(stepTimeoutSeconds));

        outbox.save(
                Order.class.getSimpleName(),
                event.orderId().toString(),
                OutboxEventType.INVENTORY_REQUEST,
                InventoryRequestEvent.of(event.orderId(), event.correlationId())
        );

        Log.infof("Payment completed step=WAITING_INVENTORY");
        inbox.markProcessed(event.eventId());
    }

    @Transactional
    public void onPaymentFailed(PaymentFailedEvent event) {
        if (!inbox.receive(event.eventId(), InboxEventType.PAYMENT_FAILED)) return;

        OrderSagaState saga = sagaRepository.findById(event.orderId());
        if (saga == null || saga.getStep() == OrderSagaState.SagaStep.CANCELLED) return;

        service.failPayment(new OrderId(event.orderId()));
        statusChangedEvent.fire(new OrderStatusChangedEvent(event.orderId(), HistoryStatus.PAYMENT_FAILED, true));
        saga.setStep(OrderSagaState.SagaStep.CANCELLED);
        sagaCompletedEvent.fire(new OrderSagaCompletedEvent(event.orderId()));
        metrics.sagaCancelled(event.orderId());

        Log.infof("Payment failed step=CANCELLED reason=%s", event.reason());
        inbox.markProcessed(event.eventId());
    }

    @Transactional
    public void onInventoryApproved(InventoryApprovedEvent event) {
        if (!inbox.receive(event.eventId(), InboxEventType.INVENTORY_APPROVED)) return;

        OrderSagaState saga = sagaRepository.findById(event.orderId());
        if (saga == null || saga.getStep() == OrderSagaState.SagaStep.CANCELLED) return;

        service.approveInventory(new OrderId(event.orderId()));
        statusChangedEvent.fire(new OrderStatusChangedEvent(event.orderId(), HistoryStatus.INVENTORY_APPROVED, true));
        saga.setStep(OrderSagaState.SagaStep.COMPLETED);
        sagaCompletedEvent.fire(new OrderSagaCompletedEvent(event.orderId()));
        metrics.sagaCompleted(event.orderId());

        Log.infof("Inventory approved step=COMPLETED");
        inbox.markProcessed(event.eventId());
    }

    @Transactional
    public void onInventoryRejected(InventoryRejectedEvent event) {
        if (!inbox.receive(event.eventId(), InboxEventType.INVENTORY_REJECTED)) return;

        OrderSagaState saga = sagaRepository.findById(event.orderId());
        if (saga == null || saga.getStep() == OrderSagaState.SagaStep.CANCELLED) return;

        service.rejectInventory(new OrderId(event.orderId()));
        statusChangedEvent.fire(new OrderStatusChangedEvent(event.orderId(), HistoryStatus.INVENTORY_REJECTED));

        outbox.save(
                Order.class.getSimpleName(),
                event.orderId().toString(),
                OutboxEventType.PAYMENT_ROLLBACK,
                PaymentRollbackEvent.of(event.orderId(), event.correlationId())
        );

        saga.setStep(OrderSagaState.SagaStep.WAITING_ROLLBACK);
        saga.setDeadline(Instant.now().plusSeconds(stepTimeoutSeconds));

        Log.infof("Inventory rejected step=WAITING_ROLLBACK reason=%s", event.reason());
        inbox.markProcessed(event.eventId());
    }

    @Transactional
    public void onPaymentRolledBack(PaymentRollbackCompletedEvent event) {
        if (!inbox.receive(event.eventId(), InboxEventType.PAYMENT_ROLLBACK_COMPLETED)) return;

        OrderSagaState saga = sagaRepository.findById(event.orderId());
        if (saga == null || saga.getStep() == OrderSagaState.SagaStep.CANCELLED) return;

        historyService.record(event.orderId(), HistoryStatus.PAYMENT_ROLLED_BACK);
        statusChangedEvent.fire(new OrderStatusChangedEvent(event.orderId(), HistoryStatus.PAYMENT_ROLLED_BACK, true));
        saga.setStep(OrderSagaState.SagaStep.CANCELLED);
        sagaCompletedEvent.fire(new OrderSagaCompletedEvent(event.orderId()));
        metrics.sagaCancelled(event.orderId());
        metrics.sagaCompensated();

        Log.infof("Payment rolled back step=CANCELLED");
        inbox.markProcessed(event.eventId());
    }

    @Transactional
    public void cancelByUser(OrderId orderId) {
        OrderSagaState saga = sagaRepository.findById(orderId.value());
        if (saga == null || saga.getStep() == OrderSagaState.SagaStep.CANCELLED
                || saga.getStep() == OrderSagaState.SagaStep.COMPLETED) return;

        boolean immediatelyTerminal = saga.getStep() != OrderSagaState.SagaStep.WAITING_INVENTORY;
        service.cancel(orderId);
        statusChangedEvent.fire(new OrderStatusChangedEvent(orderId.value(), HistoryStatus.CANCELLED, immediatelyTerminal));

        if (saga.getStep() == OrderSagaState.SagaStep.WAITING_INVENTORY) {
            outbox.save(
                    Order.class.getSimpleName(),
                    orderId.value().toString(),
                    OutboxEventType.PAYMENT_ROLLBACK,
                    PaymentRollbackEvent.of(orderId.value(), saga.getCorrelationId())
            );
            saga.setStep(OrderSagaState.SagaStep.WAITING_ROLLBACK);
            saga.setDeadline(Instant.now().plusSeconds(stepTimeoutSeconds));
            // No completion signal — onPaymentRolledBack() will fire it
        } else {
            saga.setStep(OrderSagaState.SagaStep.CANCELLED);
            sagaCompletedEvent.fire(new OrderSagaCompletedEvent(orderId.value()));
            metrics.sagaCancelled(orderId.value());
        }
    }

    private String getCorrelationId() {
        String id = correlationIdProvider.get();
        return id != null ? id : UUID.randomUUID().toString();
    }

    private boolean isUniqueConstraintViolation(Throwable t) {
        while (t != null) {
            if (t instanceof org.hibernate.exception.ConstraintViolationException) return true;
            t = t.getCause();
        }
        return false;
    }
}