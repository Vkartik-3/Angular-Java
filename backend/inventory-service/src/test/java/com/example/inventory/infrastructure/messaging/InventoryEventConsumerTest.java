package com.example.inventory.infrastructure.messaging;

import com.example.inventory.infrastructure.metrics.InventoryMetrics;
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
class InventoryEventConsumerTest {

    @Mock Emitter<com.example.order.events.avro.InventoryApprovedEvent> approvedEmitter;
    @Mock Emitter<com.example.order.events.avro.InventoryRejectedEvent> rejectedEmitter;
    @Mock InventoryMetrics metrics;

    @InjectMocks InventoryEventConsumer consumer;

    private com.example.order.events.avro.InventoryRequestEvent requestAvro;

    @BeforeEach
    void setUp() {
        consumer.setAccepted(true);
        requestAvro = com.example.order.events.avro.InventoryRequestEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOrderId(UUID.randomUUID().toString())
                .setCorrelationId("corr-1")
                .build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void process_accepted_emitsInventoryApproved() throws Exception {
        Message<com.example.order.events.avro.InventoryRequestEvent> msg = mockMessage(requestAvro);

        consumer.process(msg).toCompletableFuture().get();

        verify(approvedEmitter).send(any(Message.class));
        verify(metrics).approved();
        verify(metrics, never()).rejected();
        verify(msg).ack();
    }

    @Test
    @SuppressWarnings("unchecked")
    void process_rejected_emitsInventoryRejected() throws Exception {
        consumer.setAccepted(false);
        Message<com.example.order.events.avro.InventoryRequestEvent> msg = mockMessage(requestAvro);

        consumer.process(msg).toCompletableFuture().get();

        verify(rejectedEmitter).send(any(Message.class));
        verify(metrics).rejected();
        verify(metrics, never()).approved();
        verify(msg).ack();
    }

    @Test
    void process_crashMode_nacksMessage() throws Exception {
        consumer.setCrash(true);
        Message<com.example.order.events.avro.InventoryRequestEvent> msg = mockMessage(requestAvro);

        consumer.process(msg).toCompletableFuture().get();

        verify(metrics, never()).approved();
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