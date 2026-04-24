package com.smartcampus.exception;

/**
 * Thrown when a POST reading is attempted on a sensor in MAINTENANCE or OFFLINE status.
 * Mapped to HTTP 403 Forbidden.
 */
public class SensorUnavailableException extends RuntimeException {
    private final String sensorId;
    private final String status;

    public SensorUnavailableException(String sensorId, String status) {
        super("Sensor '" + sensorId + "' is currently '" + status +
              "' and cannot accept new readings.");
        this.sensorId = sensorId;
        this.status   = status;
    }

    public String getSensorId() { return sensorId; }
    public String getStatus()   { return status; }
}
