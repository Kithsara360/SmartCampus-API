package com.smartcampus.exception;

/**
 * Thrown when DELETE is attempted on a Room that still has Sensors assigned.
 * Mapped to HTTP 409 Conflict.
 */
public class RoomNotEmptyException extends RuntimeException {
    private final String roomId;

    public RoomNotEmptyException(String roomId) {
        super("Room '" + roomId + "' cannot be deleted: it still has active sensors assigned to it.");
        this.roomId = roomId;
    }

    public String getRoomId() { return roomId; }
}
