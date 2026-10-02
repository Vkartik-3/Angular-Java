package com.example.order.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class OrderMetrics {

    @Inject
    MeterRegistry registry;

    private Counter ordersCreated;
    private Counter sagasCompleted;
    private Counter sagasCancelled;
    private Counter sagasCompensated;
    private Counter sagasTimedOut;
    private Counter inboxDuplicates;
    private Timer sagaTimerCompleted;
    private Timer sagaTimerCancelled;

    final AtomicLong outboxPending = new AtomicLong();
    private final ConcurrentHashMap<UUID, Long> sagaStartMs = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        ordersCreated = registry.counter("orders_created_total","description", "Total orders created");
        sagasCompleted = registry.counter("sagas_completed_total", "outcome", "COMPLETED");
        sagasCancelled = registry.counter("sagas_completed_total", "outcome", "CANCELLED");
        sagasCompensated = registry.counter("sagas_compensated_total", "description", "Sagas where payment was rolled back");
        sagasTimedOut = registry.counter("sagas_timed_out_total", "description", "Sagas cancelled due to timeout");
        inboxDuplicates = registry.counter("inbox_duplicates_total", "description", "Duplicate inbox events rejected");
        sagaTimerCompleted = Timer.builder("saga_duration_seconds")
                .tag("outcome", "COMPLETED")
                .publishPercentileHistogram(true)
                .register(registry);
        sagaTimerCancelled = Timer.builder("saga_duration_seconds")
                .tag("outcome", "CANCELLED")
                .publishPercentileHistogram(true)
                .register(registry);
        Gauge.builder("outbox_pending", outboxPending, AtomicLong::get)
                .description("Unsent outbox rows waiting to be published")
                .register(registry);
    }

    public void orderCreated(UUID orderId) {
        ordersCreated.increment();
        sagaStartMs.put(orderId, System.currentTimeMillis());
    }

    public void sagaCompleted(UUID orderId) {
        sagasCompleted.increment();
        Long start = sagaStartMs.remove(orderId);
        if (start != null) {
            sagaTimerCompleted.record(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
        }
    }

    public void sagaCancelled(UUID orderId) {
        sagasCancelled.increment();
        Long start = sagaStartMs.remove(orderId);
        if (start != null) {
            sagaTimerCancelled.record(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
        }
    }

    public void sagaCompensated() {
        sagasCompensated.increment();
    }

    public void sagaTimedOut() {
        sagasTimedOut.increment();
    }

    public void inboxDuplicate() {
        inboxDuplicates.increment();
    }

    public void updateOutboxPending(long count) {
        outboxPending.set(count);
    }
}