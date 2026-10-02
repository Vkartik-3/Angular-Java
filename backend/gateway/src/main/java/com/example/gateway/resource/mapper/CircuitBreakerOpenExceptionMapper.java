package com.example.gateway.resource.mapper;

import com.example.gateway.infrastructure.observability.CorrelationIdProvider;
import com.example.gateway.resource.GatewayErrorResponse;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;

@Provider
public class CircuitBreakerOpenExceptionMapper implements ExceptionMapper<CircuitBreakerOpenException> {

    @Inject
    CorrelationIdProvider correlationIdProvider;

    @Override
    public Response toResponse(CircuitBreakerOpenException exception) {
        Log.warnf("Circuit breaker open [corrId=%s]", correlationIdProvider.get());
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(new GatewayErrorResponse(503, "CIRCUIT_OPEN",
                        "Service temporarily unavailable", correlationIdProvider.get()))
                .build();
    }
}