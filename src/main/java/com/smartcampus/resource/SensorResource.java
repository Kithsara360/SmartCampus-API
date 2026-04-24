package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Sensor;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Part 3 & 4: Sensor Resource
 * Manages /api/v1/sensors and delegates /readings to SensorReadingResource.
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    /**
     * GET /api/v1/sensors
     * GET /api/v1/sensors?type=CO2
     *
     * Returns all sensors, optionally filtered by type via @QueryParam.
     *
     * @QueryParam is superior to path-based filtering (/sensors/type/CO2) because:
     *   - Query params are semantically optional; omitting them returns all results.
     *   - Multiple filters compose naturally: ?type=CO2&status=ACTIVE.
     *   - Path segments identify resources by identity, not filter criteria.
     *   - HTTP caches treat query params as views of the same collection resource.
     */
    @GET
    public Response getSensors(@QueryParam("type") String type) {
        List<Sensor> list = new ArrayList<>(DataStore.sensors.values());
        if (type != null && !type.isBlank()) {
            list = list.stream()
                       .filter(s -> type.equalsIgnoreCase(s.getType()))
                       .collect(Collectors.toList());
        }
        return Response.ok(list).build();
    }

    /**
     * POST /api/v1/sensors
     * Registers a new sensor. Validates that the referenced roomId exists.
     *
     * @Consumes(APPLICATION_JSON): if a client sends text/plain or application/xml,
     * JAX-RS returns HTTP 415 Unsupported Media Type before this method body executes.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor) {
        if (sensor == null || sensor.getId() == null || sensor.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(Map.of("error", "Sensor 'id' is required."))
                           .build();
        }
        if (DataStore.sensors.containsKey(sensor.getId())) {
            return Response.status(Response.Status.CONFLICT)
                           .entity(Map.of("error", "Sensor '" + sensor.getId() + "' already exists."))
                           .build();
        }
        // Referential integrity: roomId must point to an existing room
        if (sensor.getRoomId() == null || !DataStore.rooms.containsKey(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException("Room", sensor.getRoomId());
        }
        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            sensor.setStatus("ACTIVE");
        }

        DataStore.sensors.put(sensor.getId(), sensor);
        DataStore.sensorReadings.put(sensor.getId(), new ArrayList<>());
        // Link sensor to its room
        DataStore.rooms.get(sensor.getRoomId()).getSensorIds().add(sensor.getId());

        return Response.status(Response.Status.CREATED).entity(sensor).build();
    }

    /**
     * GET /api/v1/sensors/{sensorId}
     */
    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = DataStore.sensors.get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(Map.of("error", "Sensor '" + sensorId + "' not found."))
                           .build();
        }
        return Response.ok(sensor).build();
    }

    /**
     * PUT /api/v1/sensors/{sensorId}
     * Updates sensor metadata. Partial updates are supported.
     */
    @PUT
    @Path("/{sensorId}")
    public Response updateSensor(@PathParam("sensorId") String sensorId, Sensor sensor) {
        Sensor existingSensor = DataStore.sensors.get(sensorId);
        if (existingSensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(Map.of("error", "Sensor '" + sensorId + "' not found."))
                           .build();
        }
        if (sensor == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(Map.of("error", "Request body is required."))
                           .build();
        }
        if (sensor.getType() != null && !sensor.getType().isBlank()) {
            existingSensor.setType(sensor.getType());
        }
        if (sensor.getStatus() != null && !sensor.getStatus().isBlank()) {
            existingSensor.setStatus(sensor.getStatus());
        }
        if (sensor.getCurrentValue() > 0 || sensor.getCurrentValue() < 0) {
            existingSensor.setCurrentValue(sensor.getCurrentValue());
        }
        return Response.ok(existingSensor).build();
    }

    /**
     * DELETE /api/v1/sensors/{sensorId}
     * Removes a sensor and unlinks it from its parent room.
     */
    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = DataStore.sensors.get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(Map.of("error", "Sensor '" + sensorId + "' not found."))
                           .build();
        }
        if (sensor.getRoomId() != null && DataStore.rooms.containsKey(sensor.getRoomId())) {
            DataStore.rooms.get(sensor.getRoomId()).getSensorIds().remove(sensorId);
        }
        DataStore.sensors.remove(sensorId);
        DataStore.sensorReadings.remove(sensorId);
        return Response.ok(Map.of(
            "message",  "Sensor '" + sensorId + "' successfully removed.",
            "sensorId", sensorId
        )).build();
    }

    /**
     * Part 4: Sub-Resource Locator
     *
     * No HTTP verb annotation — JAX-RS calls this to obtain a sub-resource instance,
     * then dispatches the actual HTTP method to it. This delegates all reading history
     * logic to SensorReadingResource, keeping each class focused and manageable.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsSubResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }
}
