package com.example.order.infrastructure.messaging;

import com.example.order.domain.event.InventoryRequestEvent;
import com.example.order.domain.event.PaymentRequestEvent;
import com.example.order.domain.event.PaymentRollbackEvent;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@ApplicationScoped
public class KafkaOrderEventPublisher {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Channel("payment-request")
    Emitter<com.example.order.events.avro.PaymentRequestEvent> paymentEmitter;

    @Channel("inventory-request")
    Emitter<com.example.order.events.avro.InventoryRequestEvent> inventoryEmitter;

    @Channel("payment-rollback")
    Emitter<com.example.order.events.avro.PaymentRollbackEvent> rollbackEmitter;

    public void publishPaymentRequest(PaymentRequestEvent event) {
        var avro = com.example.order.events.avro.PaymentRequestEvent.newBuilder()
                .setEventId(event.eventId())
                .setOrderId(event.orderId().toString())
                .setCustomerId(event.customerId().toString())
                .setAmount(event.amount().toPlainString())
                .setCorrelationId(event.correlationId())
                .build();
        send(paymentEmitter, event.orderId(), event.correlationId(), avro);
    }

    public void publishInventoryRequest(InventoryRequestEvent event) {
        var avro = com.example.order.events.avro.InventoryRequestEvent.newBuilder()
                .setEventId(event.eventId())
                .setOrderId(event.orderId().toString())
                .setCorrelationId(event.correlationId())
                .build();
        send(inventoryEmitter, event.orderId(), event.correlationId(), avro);
    }

    public void publishPaymentRollback(PaymentRollbackEvent event) {
        var avro = com.example.order.events.avro.PaymentRollbackEvent.newBuilder()
                .setEventId(event.eventId())
                .setOrderId(event.orderId().toString())
                .setCorrelationId(event.correlationId())
                .build();
        send(rollbackEmitter, event.orderId(), event.correlationId(), avro);
    }

    private <T> void send(Emitter<T> emitter, UUID key, String correlationId, T payload) {
        var metadata = OutgoingKafkaRecordMetadata.<String>builder()
                .withKey(key.toString())
                .withHeaders(new RecordHeaders()
                        .add(CORRELATION_ID_HEADER, correlationId.getBytes(StandardCharsets.UTF_8)))
                .build();
        emitter.send(Message.of(payload).addMetadata(metadata));
    }
}
