package com.example.gateway.resource.mapper;

import com.example.gateway.infrastructure.observability.CorrelationIdProvider;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Inject
    CorrelationIdProvider correlationIdProvider;

    @Override
    public Response toResponse(WebApplicationException exception) {
        Response downstream = exception.getResponse();
        Log.warnf("Downstream returned %d [corrId=%s]", downstream.getStatus(), correlationIdProvider.get());
        return Response.status(downstream.getStatus())
                .entity(downstream.getEntity())
                .build();
    }
}