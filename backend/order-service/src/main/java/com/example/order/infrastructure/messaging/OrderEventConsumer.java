package com.example.order.infrastructure.messaging;

import com.example.order.application.saga.OrderSagaOrchestrator;
import com.example.order.domain.event.InventoryApprovedEvent;
import com.example.order.domain.event.InventoryRejectedEvent;
import com.example.order.domain.event.PaymentCompletedEvent;
import com.example.order.domain.event.PaymentFailedEvent;
import com.example.order.domain.event.PaymentRollbackCompletedEvent;
import com.example.order.infrastructure.observability.CorrelationIdProvider;
import io.quarkus.logging.Log;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.MDC;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class OrderEventConsumer {

    @Inject OrderSagaOrchestrator orchestrator;
    @Inject CorrelationIdProvider correlationIdProvider;

    @Incoming("payment-completed")
    @Blocking
    @Retry(delay = 500, retryOn = OptimisticLockException.class)
    public CompletionStage<Void> onPaymentCompleted(Message<com.example.order.events.avro.PaymentCompletedEvent> message) {
        try {
            var avro = message.getPayload();
            setMDC(message, avro.getOrderId());
            Log.infof("Received payment-completed");
            orchestrator.onPaymentCompleted(new PaymentCompletedEvent(
                    avro.getEventId(), UUID.fromString(avro.getOrderId()), avro.getCorrelationId()));
            return message.ack();
        } catch (OptimisticLockException e) {
            throw e;
        } catch (Exception e) {
            Log.errorf(e, "onPaymentCompleted failed: %s", e.getMessage());
            return message.nack(e);
        } finally {
            clearMDC();
        }
    }

    @Incoming("payment-failed")
    @Blocking
    @Retry(delay = 500, retryOn = OptimisticLockException.class)
    public CompletionStage<Void> onPaymentFailed(Message<com.example.order.events.avro.PaymentFailedEvent> message) {
        try {
            var avro = message.getPayload();
            setMDC(message, avro.getOrderId());
            Log.infof("Received payment-failed");
            orchestrator.onPaymentFailed(new PaymentFailedEvent(
                    avro.getEventId(),
                    UUID.fromString(avro.getOrderId()),
                        avro.getReason(),
                    avro.getCorrelationId()));
            return message.ack();
        } catch (OptimisticLockException e) {
            throw e;
        } catch (Exception e) {
            Log.errorf(e, "onPaymentFailed failed: %s", e.getMessage());
            return message.nack(e);
        } finally {
            clearMDC();
        }
    }

    @Incoming("inventory-approved")
    @Blocking
    @Retry(delay = 500, retryOn = OptimisticLockException.class)
    public CompletionStage<Void> onInventoryApproved(Message<com.example.order.events.avro.InventoryApprovedEvent> message) {
        try {
            var avro = message.getPayload();
            setMDC(message, avro.getOrderId());
            Log.infof("Received inventory-approved");
            orchestrator.onInventoryApproved(new InventoryApprovedEvent(
                    avro.getEventId(), UUID.fromString(avro.getOrderId()), avro.getCorrelationId()));
            return message.ack();
        } catch (OptimisticLockException e) {
            throw e;
        } catch (Exception e) {
            Log.errorf(e, "onInventoryApproved failed: %s", e.getMessage());
            return message.nack(e);
        } finally {
            clearMDC();
        }
    }

    @Incoming("inventory-rejected")
    @Blocking
    @Retry(delay = 500, retryOn = OptimisticLockException.class)
    public CompletionStage<Void> onInventoryRejected(Message<com.example.order.events.avro.InventoryRejectedEvent> message) {
        try {
            var avro = message.getPayload();
            setMDC(message, avro.getOrderId());
            Log.infof("Received inventory-rejected");
            orchestrator.onInventoryRejected(new InventoryRejectedEvent(
                    avro.getEventId(),
                    UUID.fromString(avro.getOrderId()),
                    avro.getReason(),
                    avro.getCorrelationId()));
            return message.ack();
        } catch (OptimisticLockException e) {
            throw e;
        } catch (Exception e) {
            Log.errorf(e, "onInventoryRejected failed: %s", e.getMessage());
            return message.nack(e);
        } finally {
            clearMDC();
        }
    }

    @Incoming("payment-rollback-completed")
    @Blocking
    @Retry(delay = 500, retryOn = OptimisticLockException.class)
    public CompletionStage<Void> onPaymentRolledBack(Message<com.example.order.events.avro.PaymentRollbackCompletedEvent> message) {
        try {
            var avro = message.getPayload();
            setMDC(message, avro.getOrderId());
            Log.infof("Received payment-rollback-completed");
            orchestrator.onPaymentRolledBack(new PaymentRollbackCompletedEvent(
                    avro.getEventId(),
                    UUID.fromString(avro.getOrderId()),
                    avro.getCorrelationId()));
            return message.ack();
        } catch (OptimisticLockException e) {
            throw e;
        } catch (Exception e) {
            Log.errorf(e, "onPaymentRolledBack failed: %s", e.getMessage());
            return message.nack(e);
        } finally {
            clearMDC();
        }
    }

    private void setMDC(Message<?> message, String orderId) {
        String correlationId = extractCorrelationId(message);
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
            Log.warnf("Missing X-Correlation-ID header — generated fallback corrId=%s", correlationId);
        }
        correlationIdProvider.set(correlationId);
        MDC.put("orderId", orderId);
    }

    private void clearMDC() {
        MDC.clear();
    }

    private String extractCorrelationId(Message<?> message) {
        return message.getMetadata(IncomingKafkaRecordMetadata.class)
                .map(meta -> meta.getHeaders().lastHeader("X-Correlation-ID"))
                .map(header -> new String(header.value(), StandardCharsets.UTF_8))
                .orElse(null);
    }
}