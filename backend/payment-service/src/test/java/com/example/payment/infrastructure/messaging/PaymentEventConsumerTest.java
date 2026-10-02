package com.example.payment.infrastructure.messaging;

import com.example.payment.infrastructure.metrics.PaymentMetrics;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentEventConsumerTest {

    @Mock Emitter<com.example.order.events.avro.PaymentCompletedEvent> successEmitter;
    @Mock Emitter<com.example.order.events.avro.PaymentFailedEvent> failedEmitter;
    @Mock Emitter<com.example.order.events.avro.PaymentRollbackCompletedEvent> rollbackCompletedEmitter;
    @Mock PaymentMetrics metrics;

    @InjectMocks PaymentEventConsumer consumer;

    private com.example.order.events.avro.PaymentRequestEvent requestAvro;
    private com.example.order.events.avro.PaymentRollbackEvent rollbackAvro;

    @BeforeEach
    void setUp() {
        consumer.setAccepted(true);
        requestAvro = com.example.order.events.avro.PaymentRequestEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOrderId(UUID.randomUUID().toString())
                .setCustomerId(UUID.randomUUID().toString())
                .setAmount("99.99")
                .setCorrelationId("corr-1")
                .build();
        rollbackAvro = com.example.order.events.avro.PaymentRollbackEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOrderId(UUID.randomUUID().toString())
                .setCorrelationId("corr-1")
                .build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void process_accepted_recordsAcceptedMetricAndAcks() throws Exception {
        Message<com.example.order.events.avro.PaymentRequestEvent> msg = mockMessage(requestAvro);

        consumer.process(msg).toCompletableFuture().get();

        verify(successEmitter).send(any(Message.class));
        verify(metrics).accepted();
        verify(metrics, never()).rejected();
        verify(msg).ack();
    }

    @Test
    @SuppressWarnings("unchecked")
    void process_rejected_recordsRejectedMetricAndAcks() throws Exception {
        consumer.setAccepted(false);
        Message<com.example.order.events.avro.PaymentRequestEvent> msg = mockMessage(requestAvro);

        consumer.process(msg).toCompletableFuture().get();

        verify(failedEmitter).send(any(Message.class));
        verify(metrics).rejected();
        verify(metrics, never()).accepted();
        verify(msg).ack();
    }

    @Test
    @SuppressWarnings("unchecked")
    void rollback_recordsRolledBackMetricAndAcks() throws Exception {
        Message<com.example.order.events.avro.PaymentRollbackEvent> msg = mockMessage(rollbackAvro);

        consumer.rollback(msg).toCompletableFuture().get();

        verify(rollbackCompletedEmitter).send(any(Message.class));
        verify(metrics).rolledBack();
        verify(msg).ack();
    }

    @Test
    void process_crashMode_nacksMessage() throws Exception {
        consumer.setCrash(true);
        Message<com.example.order.events.avro.PaymentRequestEvent> msg = mockMessage(requestAvro);

        consumer.process(msg).toCompletableFuture().get();

        verify(metrics, never()).accepted();
        verify(msg).nack(any(Throwable.class));
    }

    @SuppressWarnings("unchecked")
    private <T> Message<T> mockMessage(T payload) {
        Message<T> msg = mock(Message.class);
        lenient().when(msg.getPayload()).thenReturn(payload);
        lenient().when(msg.ack()).thenReturn(CompletableFuture.completedFuture(null));
        lenient().when(msg.nack(any())).thenReturn(CompletableFuture.completedFuture(null));
        return msg;
    }
}