package fctreddit.impl.client.rest;

import fctreddit.api.java.Image;
import fctreddit.api.java.Result;
import fctreddit.api.rest.RestImage;
import fctreddit.impl.client.ImageClient;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import java.io.IOException;
import java.net.URI;

public class ImageRestClient extends ImageClient implements Image {


    final URI serverURI;
    final Client client;
    final ClientConfig config;

    final WebTarget target;

    public ImageRestClient(URI serverURI) {
        super(serverURI);
        this.serverURI = serverURI;

        this.config = new ClientConfig();

        config.property(ClientProperties.READ_TIMEOUT, READ_TIMEOUT);
        config.property(ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT);

        this.client = ClientBuilder.newClient(config);

        target = client.target(serverURI).path(RestImage.PATH);
    }

    @Override
    public Result<String> createImage(String userId, byte[] imageContents, String password) {
        return null;
    }

    @Override
    public Result<byte[]> getImage(String userId, String imageId) {
        return null;
    }

    public Result<Void> deleteImage(String userId, String imageId, String password) {
        Invocation.Builder builder = target.path(userId).path(imageId)
                .queryParam(RestImage.PASSWORD, password).request();
        Response r = executeOperation(builder::delete);
        int status = r.getStatus();
        if (status == Response.Status.OK.getStatusCode()) {
            return Result.ok();
        } else {
            return Result.error(getErrorCodeFrom(status));
        }
    }
}