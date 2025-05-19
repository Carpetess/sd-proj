package fctreddit.impl.client;

import fctreddit.api.java.Result;
import fctreddit.api.java.Result.ErrorCode;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import java.net.URI;
import java.util.function.Supplier;
import java.util.logging.Logger;

public abstract class Client {

	protected static final int READ_TIMEOUT = 5000;
	protected static final int CONNECT_TIMEOUT = 5000;

	protected static final int MAX_RETRIES = 10;
	protected static final int RETRY_SLEEP = 5000;
	
	private static Logger Log = Logger.getLogger(Client.class.getName());

	private final URI serverURI;
	
	public Client(URI serverURI) {
		this.serverURI = serverURI;
	}
	
	public URI getServerURI () {
		return this.serverURI;
	}


	protected Response executeOperation(Supplier<Response> func) {
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
