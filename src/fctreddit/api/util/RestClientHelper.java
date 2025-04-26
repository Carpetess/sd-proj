package fctreddit.api.util;

import fctreddit.api.java.Result.ErrorCode;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;

import java.util.function.Supplier;
import java.util.logging.Logger;

public class RestClientHelper {
    private static final int MAX_RETRIES = 500;
    private static final int RETRY_SLEEP = 500;

    private static Logger Log = Logger.getLogger(RestClientHelper.class.getName());

    public static Response executeOperation(Supplier<Response> func) {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                return func.get();
            } catch (ProcessingException x) {
                Log.info(x.getMessage());
            }
            try {
                Thread.sleep(RETRY_SLEEP);
            } catch (InterruptedException x) {
                x.printStackTrace();
            }
        }
        return null;
    }



    public static ErrorCode getErrorCodeFrom(int status) {
        return switch (status) {
            case 200, 209 -> ErrorCode.OK;
            case 409 -> ErrorCode.CONFLICT;
            case 403 -> ErrorCode.FORBIDDEN;
            case 404 -> ErrorCode.NOT_FOUND;
            case 400 -> ErrorCode.BAD_REQUEST;
            case 501 -> ErrorCode.NOT_IMPLEMENTED;
            default -> ErrorCode.INTERNAL_ERROR;
        };
    }

}
