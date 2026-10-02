package com.example.gateway.infrastructure.filter;

import com.example.gateway.infrastructure.observability.CorrelationIdProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

@ApplicationScoped
public class OutgoingCorrelationIdFilter implements ClientRequestFilter {

    private static final String HEADER = "X-Correlation-ID";

    @Inject
    CorrelationIdProvider correlationIdProvider;

    @Override
    public void filter(ClientRequestContext requestContext) {
        String id = correlationIdProvider.get();
        if (id != null) {
            requestContext.getHeaders().putSingle(HEADER, id);
        }
    }
}