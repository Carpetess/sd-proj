package fctreddit.api.clients.ImageClients;

import fctreddit.api.util.RestClientHelper;
import fctreddit.api.java.Result;
import fctreddit.api.rest.RestImage;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;

import java.net.URI;

import static org.glassfish.jersey.client.ClientProperties.CONNECT_TIMEOUT;
import static org.glassfish.jersey.client.ClientProperties.READ_TIMEOUT;

public class ImageRestClient extends ImageClient {


    final URI serverURI;
    final Client client;
    final ClientConfig config;

    final WebTarget target;

    public ImageRestClient(URI serverURI) {
        this.serverURI = serverURI;

        this.config = new ClientConfig();

        config.property(READ_TIMEOUT, 5000);
        config.property(CONNECT_TIMEOUT, 5000);

        this.client = ClientBuilder.newClient(config);

        target = client.target(serverURI).path(RestImage.PATH);
    }

    @Override
    public Result<Void> deleteImage(String userId, String imageId, String password) {
        Invocation.Builder builder = target.path( userId ).path( imageId )
                .queryParam(RestImage.PASSWORD, password).request();
        Response r = RestClientHelper.executeOperation(builder::delete);
        int status = r.getStatus();
        if (status == Response.Status.OK.getStatusCode()) {
            return Result.ok();
        } else {
            return Result.error(RestClientHelper.getErrorCodeFrom(status));
        }
    }
}
