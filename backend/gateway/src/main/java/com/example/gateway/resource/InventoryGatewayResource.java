package com.example.gateway.resource;

import com.example.gateway.client.InventoryServiceClient;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/api/inventory")
@Produces(MediaType.TEXT_PLAIN)
@RolesAllowed("admin")
public class InventoryGatewayResource {

    @Inject
    @RestClient
    InventoryServiceClient inventoryServiceClient;

    @Inject
    GatewayResponseForwarder forwarder;

    @GET
    @Path("/mode")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMode() {
        return forwarder.forward(inventoryServiceClient.getMode());
    }

    @PUT
    @Path("/mode")
    public Response setMode(@QueryParam("accept") boolean accept) {
        return forwarder.forward(inventoryServiceClient.setMode(accept));
    }

    @GET
    @Path("/delay")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDelay() {
        return forwarder.forward(inventoryServiceClient.getDelay());
    }

    @PUT
    @Path("/delay")
    public Response setDelay(@QueryParam("seconds") int seconds) {
        return forwarder.forward(inventoryServiceClient.setDelay(seconds));
    }

    @GET
    @Path("/crash")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCrash() {
        return forwarder.forward(inventoryServiceClient.getCrash());
    }

    @PUT
    @Path("/crash")
    public Response setCrash(@QueryParam("enabled") boolean enabled) {
        return forwarder.forward(inventoryServiceClient.setCrash(enabled));
    }

}
