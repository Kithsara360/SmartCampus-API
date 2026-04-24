package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Part 4: Sub-Resource for sensor reading history.
 *
 * This class has NO top-level @Path annotation. It is NOT registered directly with JAX-RS.
 * Instead, SensorResource's sub-resource locator method instantiates it, providing it with
 * the sensorId context for the current request.
 *
 * Benefits of this pattern:
 *   - Single Responsibility: each class handles exactly one level of the resource hierarchy.
 *   - Testability: this class can be unit-tested by constructing it with a test sensorId.
 *   - Scalability: a real API might have 10+ sub-resources per sensor; keeping them
 *     in separate classes prevents a monolithic, unmanageable controller.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    /**
     * GET /api/v1/sensors/{sensorId}/readings
     * Returns the full reading history for the sensor.
     */
    @GET
    public Response getReadings() {
        if (!DataStore.sensors.containsKey(sensorId)) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(Map.of("error", "Sensor '" + sensorId + "' not found."))
                           .build();
        }
        List<SensorReading> readings = DataStore.sensorReadings.getOrDefault(sensorId, new ArrayList<>());
        return Response.ok(readings).build();
    }

    /**
     * POST /api/v1/sensors/{sensorId}/readings
     * Appends a new reading.
     * - Blocked if sensor is MAINTENANCE or OFFLINE (throws SensorUnavailableException → 403).
     * - Side effect: updates the parent sensor's currentValue to maintain consistency.
     */
    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = DataStore.sensors.get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(Map.of("error", "Sensor '" + sensorId + "' not found."))
                           .build();
        }
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus()) ||
            "OFFLINE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(sensorId, sensor.getStatus());
        }
        if (reading == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(Map.of("error", "Request body with 'value' is required."))
                           .build();
        }

        SensorReading newReading = new SensorReading(reading.getValue());
        DataStore.sensorReadings.computeIfAbsent(sensorId, k -> new ArrayList<>()).add(newReading);

        // Side effect: keep parent sensor's currentValue in sync
        sensor.setCurrentValue(newReading.getValue());

        return Response.status(Response.Status.CREATED).entity(newReading).build();
    }
}
