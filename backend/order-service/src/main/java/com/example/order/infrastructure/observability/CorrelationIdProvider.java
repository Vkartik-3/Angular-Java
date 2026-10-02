package com.example.order.infrastructure.observability;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.MDC;

@ApplicationScoped
public class CorrelationIdProvider {

    private static final String KEY = "correlationId";

    public void set(String id) {
        MDC.put(KEY, id);
    }

    public String get() {
        return (String) MDC.get(KEY);
    }

    public void clear() {
        MDC.remove(KEY);
    }
}
