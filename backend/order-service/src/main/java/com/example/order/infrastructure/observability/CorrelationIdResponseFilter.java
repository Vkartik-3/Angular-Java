package com.example.order.infrastructure.observability;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.MDC;

@Provider
@ApplicationScoped
public class CorrelationIdResponseFilter implements ContainerResponseFilter {

    private static final String HEADER = "X-Correlation-ID";

    @Inject
    CorrelationIdProvider correlationIdProvider;

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        String id = correlationIdProvider.get();

        if (id != null) {
            responseContext.getHeaders().putSingle(HEADER, id);
        }

        correlationIdProvider.clear();
        MDC.remove("orderId");
    }
}
