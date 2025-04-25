package fctreddit.api.clients.UserClients;

import fctreddit.api.data.User;
import fctreddit.api.util.RestClientHelper;
import fctreddit.api.java.Result;
import fctreddit.api.rest.RestUsers;
import jakarta.ws.rs.client.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;

import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

import static org.glassfish.jersey.client.ClientProperties.CONNECT_TIMEOUT;
import static org.glassfish.jersey.client.ClientProperties.READ_TIMEOUT;

public class UsersRestClient extends UsersClient {


    final URI serverURI;
    final Client client;
    final ClientConfig config;

    final WebTarget target;
    private static final Logger Log = Logger.getLogger(UsersRestClient.class.getName());

    public UsersRestClient( URI serverURI ) {
        this.serverURI = serverURI;

        this.config = new ClientConfig();

        config.property( READ_TIMEOUT, 5000);
        config.property( CONNECT_TIMEOUT, 5000);

        this.client = ClientBuilder.newClient(config);

        target = client.target( serverURI ).path( RestUsers.PATH );
    }
    @Override
    public Result<User> getUser(String userId, String password) {

        Invocation.Builder builder = target.path( userId )
                .queryParam(RestUsers.PASSWORD, password ).request()
                .accept(MediaType.APPLICATION_JSON);
        Response r = RestClientHelper.executeOperation(builder::get);
        int status = r.getStatus();
        if ( status == Response.Status.OK.getStatusCode() && r.hasEntity() ) {
            return Result.ok(r.readEntity(User.class));
        } else {
            return Result.error(RestClientHelper.getErrorCodeFrom(status));
        }
    }


    @Override
    public Result<User> updateUser(String userId, String password, User user) {
        Invocation.Builder builder = target.path( userId )
                .queryParam(RestUsers.PASSWORD, password ).request()
                .accept(MediaType.APPLICATION_JSON);
        Response r = RestClientHelper.executeOperation(() -> builder.put(Entity.entity(user, MediaType.APPLICATION_JSON)));

        int status = r.getStatus();
        if ( status == Response.Status.NO_CONTENT.getStatusCode() && r.hasEntity()) {
            return Result.ok(r.readEntity(User.class)) ;
        } else {
            return Result.error(RestClientHelper.getErrorCodeFrom(status));
        }
    }

    @Override
    public Result<User> deleteUser(String userId, String password) {
        Invocation.Builder builder = target.path( userId )
                .queryParam(RestUsers.PASSWORD, password ).request()
                .accept(MediaType.APPLICATION_JSON);
        Response r = RestClientHelper.executeOperation(builder::delete);
        int status = r.getStatus();
        if ( status == Response.Status.OK.getStatusCode() && r.hasEntity() ) {
            return Result.ok(r.readEntity(User.class));
        } else {
            return Result.error(RestClientHelper.getErrorCodeFrom(status));
        }
    }

    @Override
    public Result<String> createUser(User user) {
        Invocation.Builder builder = target.request()
                .accept(MediaType.APPLICATION_JSON);
        Response r = RestClientHelper.executeOperation(() -> builder.post(Entity.entity(user, MediaType.APPLICATION_JSON)));
        int status = r.getStatus();
        if ( status == Response.Status.OK.getStatusCode() && r.hasEntity() ) {
            return Result.ok(r.readEntity(String.class));
        } else {
            return Result.error(RestClientHelper.getErrorCodeFrom(status));
        }
    }

    @Override
    public Result<List<User>> searchUsers(String pattern) {
        Invocation.Builder builder = target
                .queryParam(RestUsers.QUERY, pattern).request()
                .accept(MediaType.APPLICATION_OCTET_STREAM);
        Response r = RestClientHelper.executeOperation(builder::get);
        int status = r.getStatus();
        if ( status == Response.Status.OK.getStatusCode() && r.hasEntity() ) {
            return Result.ok(r.readEntity(List.class));
        } else {
            return Result.error(RestClientHelper.getErrorCodeFrom(status));
        }

    }
}
