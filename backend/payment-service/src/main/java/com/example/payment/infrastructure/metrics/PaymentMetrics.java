package com.example.payment.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PaymentMetrics {

    @Inject
    MeterRegistry registry;

    private Counter accepted;
    private Counter rejected;
    private Counter rolledBack;

    @PostConstruct
    void init() {
        accepted = registry.counter("payments_processed_total", "result", "accepted");
        rejected = registry.counter("payments_processed_total", "result", "rejected");
        rolledBack = registry.counter("payment_rollbacks_total");
    }

    public void accepted() { accepted.increment(); }
    public void rejected() { rejected.increment(); }
    public void rolledBack() { rolledBack.increment(); }
}