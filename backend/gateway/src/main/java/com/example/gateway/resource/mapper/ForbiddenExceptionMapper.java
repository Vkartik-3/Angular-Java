package com.example.gateway.resource.mapper;

import com.example.gateway.infrastructure.observability.CorrelationIdProvider;
import com.example.gateway.resource.GatewayErrorResponse;
import io.quarkus.security.ForbiddenException;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ForbiddenExceptionMapper implements ExceptionMapper<ForbiddenException> {

    @Inject
    CorrelationIdProvider correlationIdProvider;

    @Override
    public Response toResponse(ForbiddenException exception) {
        return Response.status(Response.Status.FORBIDDEN)
                .entity(new GatewayErrorResponse(403, "FORBIDDEN",
                        "Insufficient permissions", correlationIdProvider.get()))
                .build();
    }
}