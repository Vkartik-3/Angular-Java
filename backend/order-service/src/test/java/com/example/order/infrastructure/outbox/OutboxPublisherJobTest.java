package com.example.order.infrastructure.outbox;

import com.example.order.domain.event.InventoryRequestEvent;
import com.example.order.domain.event.PaymentRequestEvent;
import com.example.order.domain.event.PaymentRollbackEvent;
import com.example.order.infrastructure.messaging.KafkaOrderEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherJobTest {

    @Mock OutboxRepository repository;
    @Mock KafkaOrderEventPublisher kafka;

    @InjectMocks OutboxPublisherJob job;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        setField("batchSize", 10);
        setField("maxRetries", 5);
        setField("retentionDays", 30);
        setField("objectMapper", MAPPER);
    }

    @Test
    void publish_pendingEvents_publishesAll() {
        UUID orderId = UUID.randomUUID();
        OutboxEventEntity event = pending(orderId, OutboxEventType.INVENTORY_REQUEST,
                toJson(InventoryRequestEvent.of(orderId, "corr-1")));
        when(repository.findUnprocessed(10, 5)).thenReturn(List.of(event));

        job.publish();

        verify(kafka).publishInventoryRequest(any());
        assertTrue(event.isProcessed());
    }

    @Test
    void publish_noEvents_kafkaNotCalled() {
        when(repository.findUnprocessed(10, 5)).thenReturn(List.of());

        job.publish();

        verify(kafka, never()).publishPaymentRequest(any());
        verify(kafka, never()).publishInventoryRequest(any());
        verify(kafka, never()).publishPaymentRollback(any());
    }

    @Test
    void processPublish_paymentRequest_routesAndMarksProcessed() {
        UUID orderId = UUID.randomUUID();
        PaymentRequestEvent e = PaymentRequestEvent.of(orderId, UUID.randomUUID(), new BigDecimal("99.99"), "corr-1");
        OutboxEventEntity event = pending(orderId, OutboxEventType.PAYMENT_REQUEST, toJson(e));

        job.processPublish(event);

        verify(kafka).publishPaymentRequest(any(PaymentRequestEvent.class));
        assertTrue(event.isProcessed());
        assertNotNull(event.getProcessedAt());
    }

    @Test
    void processPublish_inventoryRequest_routesAndMarksProcessed() {
        UUID orderId = UUID.randomUUID();
        OutboxEventEntity event = pending(orderId, OutboxEventType.INVENTORY_REQUEST,
                toJson(InventoryRequestEvent.of(orderId, "corr-1")));

        job.processPublish(event);

        verify(kafka).publishInventoryRequest(any(InventoryRequestEvent.class));
        assertTrue(event.isProcessed());
    }

    @Test
    void processPublish_paymentRollback_routesAndMarksProcessed() {
        UUID orderId = UUID.randomUUID();
        OutboxEventEntity event = pending(orderId, OutboxEventType.PAYMENT_ROLLBACK,
                toJson(PaymentRollbackEvent.of(orderId, "corr-1")));

        job.processPublish(event);

        verify(kafka).publishPaymentRollback(any(PaymentRollbackEvent.class));
        assertTrue(event.isProcessed());
    }

    @Test
    void processPublish_kafkaThrows_incrementsRetryAndRecordsError() {
        UUID orderId = UUID.randomUUID();
        OutboxEventEntity event = pending(orderId, OutboxEventType.INVENTORY_REQUEST,
                toJson(InventoryRequestEvent.of(orderId, "corr-1")));
        doThrow(new RuntimeException("Kafka unavailable")).when(kafka).publishInventoryRequest(any());

        job.processPublish(event);

        assertEquals(1, event.getRetryCount());
        assertEquals("Kafka unavailable", event.getLastError());
        assertFalse(event.isProcessed());
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    private OutboxEventEntity pending(UUID aggregateId, OutboxEventType type, String payload) {
        return OutboxEventEntity.builder()
                .id(UUID.randomUUID().toString())
                .aggregateId(aggregateId.toString())
                .aggregateType("Order")
                .eventType(type)
                .payload(payload)
                .retryCount(0)
                .processed(false)
                .build();
    }

    private String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(String name, Object value) throws Exception {
        var field = OutboxPublisherJob.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(job, value);
    }
}
