package fctreddit.api.rest;

import fctreddit.api.java.Result;
import jakarta.ws.rs.core.Response;

public interface Resource {
    Response.Status errorCodeToStatus(Result<?> result);
}
