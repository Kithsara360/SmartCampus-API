package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Part 2: Room Management Resource
 * Manages the /api/v1/rooms collection.
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    /**
     * GET /api/v1/rooms
     * Returns all rooms.
     */
    @GET
    public Response getAllRooms() {
        List<Room> roomList = new ArrayList<>(DataStore.rooms.values());
        return Response.ok(roomList).build();
    }

    /**
     * POST /api/v1/rooms
     * Creates a new room. Returns 201 Created with the created room body.
     */
    @POST
    public Response createRoom(Room room) {
        if (room == null || room.getId() == null || room.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(Map.of("error", "Room 'id' is required."))
                           .build();
        }
        if (room.getName() == null || room.getName().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(Map.of("error", "Room 'name' is required."))
                           .build();
        }
        if (room.getCapacity() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(Map.of("error", "Room 'capacity' is required."))
                           .build();
        }
        if (DataStore.rooms.containsKey(room.getId())) {
            return Response.status(Response.Status.CONFLICT)
                           .entity(Map.of("error", "Room '" + room.getId() + "' already exists."))
                           .build();
        }
        if (room.getSensorIds() == null) {
            room.setSensorIds(new ArrayList<>());
        }
        DataStore.rooms.put(room.getId(), room);
        return Response.status(Response.Status.CREATED).entity(room).build();
    }

    /**
     * PUT /api/v1/rooms/{roomId}
     * Updates room metadata. Partial updates are supported.
     */
    @PUT
    @Path("/{roomId}")
    public Response updateRoom(@PathParam("roomId") String roomId, Room room) {
        Room existingRoom = DataStore.rooms.get(roomId);
        if (existingRoom == null) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(Map.of("error", "Room '" + roomId + "' not found."))
                           .build();
        }
        if (room == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(Map.of("error", "Request body is required."))
                           .build();
        }
        if (room.getName() != null && !room.getName().isBlank()) {
            existingRoom.setName(room.getName());
        }
        if (room.getCapacity() != null) {
            existingRoom.setCapacity(room.getCapacity());
        }
        return Response.ok(existingRoom).build();
    }

    /**
     * GET /api/v1/rooms/{roomId}
     * Returns detailed metadata for a specific room.
     */
    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = DataStore.rooms.get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(Map.of("error", "Room '" + roomId + "' not found."))
                           .build();
        }
        return Response.ok(room).build();
    }

    /**
     * DELETE /api/v1/rooms/{roomId}
     * Decommissions a room. Blocked if the room still has sensors assigned.
     *
     * Idempotency: DELETE is idempotent — the server state is identical after one or
     * many identical DELETE calls (the room does not exist). Response codes may differ
     * (200 then 404), but the resource state is the same. Clients can safely retry on
     * network timeout without causing unintended side effects.
     */
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = DataStore.rooms.get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(Map.of("error", "Room '" + roomId + "' not found."))
                           .build();
        }
        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId);
        }
        DataStore.rooms.remove(roomId);
        return Response.ok(Map.of(
            "message", "Room '" + roomId + "' successfully decommissioned.",
            "roomId",  roomId
        )).build();
    }
}
