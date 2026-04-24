package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {

    @Override
    public Response toResponse(RoomNotEmptyException ex) {
        return Response
            .status(Response.Status.CONFLICT)
            .entity(new ErrorResponse(409, "Conflict", ex.getMessage()))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }
}
