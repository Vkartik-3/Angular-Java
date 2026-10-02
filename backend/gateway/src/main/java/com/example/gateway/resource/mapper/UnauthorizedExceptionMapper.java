package com.example.gateway.resource.mapper;

import com.example.gateway.infrastructure.observability.CorrelationIdProvider;
import com.example.gateway.resource.GatewayErrorResponse;
import io.quarkus.security.UnauthorizedException;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class UnauthorizedExceptionMapper implements ExceptionMapper<UnauthorizedException> {

    @Inject
    CorrelationIdProvider correlationIdProvider;

    @Override
    public Response toResponse(UnauthorizedException exception) {
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new GatewayErrorResponse(401, "UNAUTHORIZED",
                        "Authentication required", correlationIdProvider.get()))
                .build();
    }
}