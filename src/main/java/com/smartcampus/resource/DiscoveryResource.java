package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Part 1: Discovery endpoint at GET /api/v1
 *
 * Returns API metadata: version, contact info, and navigable links to primary collections.
 * The inclusion of links is a lightweight implementation of HATEOAS — making the API
 * self-describing and navigable without external documentation.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("name",        "Smart Campus Sensor & Room Management API");
        response.put("version",     "1.0.0");
        response.put("description", "RESTful API for managing university campus rooms and IoT sensors.");
        response.put("contact", Map.of(
            "name",  "Campus IT Administration",
            "email", "admin@smartcampus.ac.uk"
        ));
        response.put("links", Map.of(
            "self",    "/api/v1",
            "rooms",   "/api/v1/rooms",
            "sensors", "/api/v1/sensors"
        ));
        return Response.ok(response).build();
    }
}
