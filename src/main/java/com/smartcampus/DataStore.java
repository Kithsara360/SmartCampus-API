package com.smartcampus;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralised in-memory data store using ConcurrentHashMap for thread safety.
 *
 * Because JAX-RS creates a new Resource instance per request by default, all
 * mutable state MUST live outside the resource class. ConcurrentHashMap provides
 * atomic operations, preventing race conditions when multiple requests read/write
 * simultaneously without the overhead of explicit synchronization blocks.
 */
public class DataStore {

    private DataStore() {}

    public static final ConcurrentHashMap<String, Room>          rooms          = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, Sensor>        sensors        = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, List<SensorReading>> sensorReadings = new ConcurrentHashMap<>();

    // Seed demo data
    static {
        Room r1 = new Room("LIB-301", "Library Quiet Study", 50);
        Room r2 = new Room("LAB-101", "Computer Science Lab", 30);
        Room r3 = new Room("HALL-A",  "Main Lecture Hall",   200);
        rooms.put(r1.getId(), r1);
        rooms.put(r2.getId(), r2);
        rooms.put(r3.getId(), r3);

        Sensor s1 = new Sensor("TEMP-001",  "Temperature", "ACTIVE",      22.5,  "LIB-301");
        Sensor s2 = new Sensor("CO2-001",   "CO2",         "ACTIVE",      412.0, "LIB-301");
        Sensor s3 = new Sensor("OCC-001",   "Occupancy",   "MAINTENANCE", 0.0,   "LAB-101");
        Sensor s4 = new Sensor("LIGHT-001", "Light",       "ACTIVE",      320.0, "HALL-A");
        sensors.put(s1.getId(), s1);
        sensors.put(s2.getId(), s2);
        sensors.put(s3.getId(), s3);
        sensors.put(s4.getId(), s4);

        r1.getSensorIds().add(s1.getId());
        r1.getSensorIds().add(s2.getId());
        r2.getSensorIds().add(s3.getId());
        r3.getSensorIds().add(s4.getId());

        sensorReadings.put(s1.getId(), new ArrayList<>());
        sensorReadings.put(s2.getId(), new ArrayList<>());
        sensorReadings.put(s3.getId(), new ArrayList<>());
        sensorReadings.put(s4.getId(), new ArrayList<>());
    }
}
