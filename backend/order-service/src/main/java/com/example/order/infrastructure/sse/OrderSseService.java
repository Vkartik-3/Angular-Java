package com.example.order.infrastructure.sse;

import com.example.order.domain.event.OrderStatusChangedEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Pushes saga status updates to SSE clients (see OrderResource#streamStatus).
 * <p>
 * Multiple replicas: the pod holding the SSE connection may differ from the pod
 * processing the saga. Status changes are broadcast via order-status-fanout —
 * each replica subscribes with a unique consumer group so every pod receives
 * every message and checks its own local emitter map.
 * <p>
 * Status and sagaCompleted() are encoded in one message deliberately. Separate
 * messages (status, then a completion marker) produced unreliable ordering with
 * {@code @Observes(during = AFTER_SUCCESS)} — the completion marker sometimes
 * arrived (and closed the stream) before the final status, silently dropping it.
 */
@ApplicationScoped
public class OrderSseService {

    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<MultiEmitter<? super String>>> emitters =
            new ConcurrentHashMap<>();

    @Channel("order-status-fanout-out")
    Emitter<String> fanoutEmitter;

    void onStatusChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) OrderStatusChangedEvent event) {
        String payload = event.orderId() + "|" + event.status().name() + "|" + event.sagaCompleted();
        fanoutEmitter.send(payload);
    }

    @Incoming("order-status-fanout-in")
    void onFanoutMessage(String message) {
        String[] parts = message.split("\\|", 3);
        if (parts.length < 3) return;
        UUID orderId = UUID.fromString(parts[0]);
        String status = parts[1];
        boolean completed = Boolean.parseBoolean(parts[2]);

        var list = completed ? emitters.remove(orderId) : emitters.get(orderId);
        if (list == null) return;

        list.forEach(e -> e.emit(status));
        if (completed) list.forEach(MultiEmitter::complete);
    }

    public Multi<String> stream(UUID orderId) {
        return Multi.createFrom().emitter(emitter -> {
            emitters.computeIfAbsent(orderId, _ -> new CopyOnWriteArrayList<>()).add(emitter);
            emitter.onTermination(() -> {
                var list = emitters.get(orderId);
                if (list != null) {
                    list.remove(emitter);
                    if (list.isEmpty()) emitters.remove(orderId);
                }
            });
        });
    }
}