package com.example.inventory.infrastructure.messaging;

import com.example.inventory.infrastructure.metrics.InventoryMetrics;
import io.quarkus.logging.Log;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.MDC;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class InventoryEventConsumer {

    @Inject
    InventoryMetrics metrics;

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Channel("inventory-approved")
    Emitter<com.example.order.events.avro.InventoryApprovedEvent> approvedEmitter;

    @Channel("inventory-rejected")
    Emitter<com.example.order.events.avro.InventoryRejectedEvent> rejectedEmitter;

    @ConfigProperty(name = "app.inventory.accepted", defaultValue = "true")
    boolean accepted;

    @ConfigProperty(name = "app.inventory.delay", defaultValue = "0")
    int delay;

    @ConfigProperty(name = "app.inventory.crash", defaultValue = "false")
    boolean crash;

    public boolean isAccepted() { return accepted; }

    public void setAccepted(boolean accepted) {
        Log.infof("Inventory mode changed: accept=%s", accepted);
        this.accepted = accepted;
    }

    public int getDelay() { return delay; }

    public void setDelay(int delay) {
        Log.infof("Inventory delay changed: %ds", delay);
        this.delay = delay;
    }

    public boolean isCrash() { return crash; }

    public void setCrash(boolean crash) {
        Log.infof("Inventory crash mode changed: crash=%s", crash);
        this.crash = crash;
    }

    private void sleep() {
        if (delay > 0) try { Thread.sleep(delay * 1000L); } catch (InterruptedException ignored) {}
    }

    @Incoming("inventory-request")
    @io.smallrye.reactive.messaging.annotations.Blocking
    public CompletionStage<Void> process(Message<com.example.order.events.avro.InventoryRequestEvent> message) {
        try {
            if (crash) throw new RuntimeException("Simulated consumer crash");
            sleep();
            var avro = message.getPayload();
            String orderId = avro.getOrderId();
            String correlationId = avro.getCorrelationId();
            MDC.put("correlationId", correlationId);
            MDC.put("orderId", orderId);

            if (accepted) {
                metrics.approved();
                Log.infof("Inventory approved");
                approvedEmitter.send(Message.of(
                        com.example.order.events.avro.InventoryApprovedEvent.newBuilder()
                                .setEventId(UUID.randomUUID().toString())
                                .setOrderId(orderId)
                                .setCorrelationId(correlationId)
                                .build())
                        .addMetadata(key(orderId, correlationId)));
            } else {
                metrics.rejected();
                Log.infof("Inventory rejected");
                rejectedEmitter.send(Message.of(
                        com.example.order.events.avro.InventoryRejectedEvent.newBuilder()
                                .setEventId(UUID.randomUUID().toString())
                                .setOrderId(orderId)
                                .setReason("Out of stock")
                                .setCorrelationId(correlationId)
                                .build())
                        .addMetadata(key(orderId, correlationId)));
            }
            return message.ack();
        } catch (Exception e) {
            Log.errorf("Inventory consumer failure: %s", e.getMessage());
            return message.nack(e);
        } finally {
            MDC.remove("correlationId");
            MDC.remove("orderId");
        }
    }

    private OutgoingKafkaRecordMetadata<String> key(String orderId, String correlationId) {
        return OutgoingKafkaRecordMetadata.<String>builder()
                .withKey(orderId)
                .withHeaders(new RecordHeaders()
                        .add(CORRELATION_ID_HEADER, correlationId.getBytes(StandardCharsets.UTF_8)))
                .build();
    }
}