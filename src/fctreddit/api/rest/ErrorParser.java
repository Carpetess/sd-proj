package fctreddit.api.rest;

import fctreddit.api.java.Result;
import jakarta.ws.rs.core.Response;

public class ErrorParser {

    protected static Response.Status errorCodeToStatus(Result.ErrorCode error) {
        return switch (error) {
            case NOT_FOUND -> Response.Status.NOT_FOUND;
            case CONFLICT -> Response.Status.CONFLICT;
            case FORBIDDEN -> Response.Status.FORBIDDEN;
            case NOT_IMPLEMENTED -> Response.Status.NOT_IMPLEMENTED;
            case BAD_REQUEST -> Response.Status.BAD_REQUEST;
            default -> Response.Status.INTERNAL_SERVER_ERROR;
        };
    }
}
