package com.example.gateway.infrastructure.filter;

import com.example.gateway.infrastructure.observability.CorrelationIdProvider;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

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

        Log.infof("%s %s → %d", requestContext.getMethod(), requestContext.getUriInfo().getRequestUri().getPath(), responseContext.getStatus());
        correlationIdProvider.clear();
    }
}
