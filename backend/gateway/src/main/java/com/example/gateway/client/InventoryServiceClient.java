package com.example.gateway.client;

import com.example.gateway.infrastructure.filter.OutgoingCorrelationIdFilter;
import com.example.gateway.infrastructure.filter.OutgoingJwtFilter;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "inventory-service")
@RegisterProvider(OutgoingCorrelationIdFilter.class)
@RegisterProvider(OutgoingJwtFilter.class)
@Path("/inventory")
@Produces(MediaType.TEXT_PLAIN)
public interface InventoryServiceClient {

    @GET
    @Path("/mode")
    @Produces(MediaType.APPLICATION_JSON)
    @Retry(delay = 200)
    @CircuitBreaker(requestVolumeThreshold = 10, delay = 5000, successThreshold = 2)
    Response getMode();

    @PUT
    @Path("/mode")
    @Retry(delay = 200)
    @CircuitBreaker(requestVolumeThreshold = 10, delay = 5000, successThreshold = 2)
    Response setMode(@QueryParam("accept") boolean accept);

    @GET
    @Path("/delay")
    @Produces(MediaType.APPLICATION_JSON)
    @Retry(delay = 200)
    @CircuitBreaker(requestVolumeThreshold = 10, delay = 5000, successThreshold = 2)
    Response getDelay();

    @PUT
    @Path("/delay")
    @Retry(delay = 200)
    @CircuitBreaker(requestVolumeThreshold = 10, delay = 5000, successThreshold = 2)
    Response setDelay(@QueryParam("seconds") int seconds);

    @GET
    @Path("/crash")
    @Produces(MediaType.APPLICATION_JSON)
    @Retry(delay = 200)
    @CircuitBreaker(requestVolumeThreshold = 10, delay = 5000, successThreshold = 2)
    Response getCrash();

    @PUT
    @Path("/crash")
    @Retry(delay = 200)
    @CircuitBreaker(requestVolumeThreshold = 10, delay = 5000, successThreshold = 2)
    Response setCrash(@QueryParam("enabled") boolean enabled);
}
