package com.example.gateway.resource;

import com.example.gateway.client.OrderServiceClient;
import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.UUID;

@Authenticated
@Path("/api/orders")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OrderGatewayResource {

    @Inject
    @RestClient
    OrderServiceClient orderServiceClient;

    @Inject
    GatewayResponseForwarder forwarder;

    @POST
    public Response create(String body, @HeaderParam("Idempotency-Key") String idempotencyKey) {
        return forwarder.forward(orderServiceClient.create(body, idempotencyKey));
    }

    @GET
    public Response getAll() {
        return forwarder.forward(orderServiceClient.getAll());
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") UUID id) {
        return forwarder.forward(orderServiceClient.getById(id));
    }

    @PUT
    @Path("/{id}/cancel")
    public Response cancel(@PathParam("id") UUID id) {
        return forwarder.forward(orderServiceClient.cancel(id));
    }

    @GET
    @Path("/{id}/events")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<String> streamStatus(@PathParam("id") UUID id) {
        return orderServiceClient.streamStatus(id);
    }

}
