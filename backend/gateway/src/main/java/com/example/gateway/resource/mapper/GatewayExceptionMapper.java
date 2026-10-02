package com.example.gateway.resource.mapper;

import com.example.gateway.infrastructure.observability.CorrelationIdProvider;
import com.example.gateway.resource.GatewayErrorResponse;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GatewayExceptionMapper implements ExceptionMapper<Exception> {

    @Inject
    CorrelationIdProvider correlationIdProvider;

    @Override
    public Response toResponse(Exception exception) {
        Log.errorf(exception, "Unexpected gateway error [corrId=%s]", correlationIdProvider.get());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new GatewayErrorResponse(500, "INTERNAL_ERROR",
                        "Unexpected error", correlationIdProvider.get()))
                .build();
    }
}