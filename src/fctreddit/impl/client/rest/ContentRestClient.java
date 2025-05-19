package fctreddit.impl.client.rest;

import fctreddit.api.data.Post;
import fctreddit.api.java.Content;
import fctreddit.api.java.Result;
import fctreddit.api.rest.RestContent;
import fctreddit.impl.client.ContentClient;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import java.net.URI;
import java.util.List;

public class ContentRestClient extends ContentClient implements Content {

    final URI serverURI;
    final Client client;
    final ClientConfig config;

    final WebTarget target;

    public ContentRestClient ( URI serverURI ) {
        super(serverURI);
        this.serverURI = serverURI;

        this.config = new ClientConfig();

        config.property(ClientProperties.READ_TIMEOUT, READ_TIMEOUT);
        config.property( ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT);

        this.client = ClientBuilder.newClient(config);

        target = client.target( serverURI ).path( RestContent.PATH );
    }

    @Override
    public Result<Void> removeAllUserVotes(String userId, String password) {
        Invocation.Builder builder = target.path( userId ).path( "votes")
                .queryParam(RestContent.PASSWORD, password).request();

        Response r = executeOperation(builder::delete);
        int status = r.getStatus();
        if (status == Response.Status.OK.getStatusCode()) {
            return Result.ok();
        } else {
            return Result.error(getErrorCodeFrom(status));
        }
    }

    @Override
    public Result<String> createPost(Post post, String userPassword) {
        return null;
    }

    @Override
    public Result<List<String>> getPosts(long timestamp, String sortOrder) {
        return null;
    }

    @Override
    public Result<Post> getPost(String postId) {
        return null;
    }

    @Override
    public Result<List<String>> getPostAnswers(String postId, long maxTimeout) {
        return null;
    }

    @Override
    public Result<Post> updatePost(String postId, String userPassword, Post post) {
        return null;
    }

    @Override
    public Result<Void> deletePost(String postId, String userPassword) {
        return null;
    }

    @Override
    public Result<Void> upVotePost(String postId, String userId, String userPassword) {
        return null;
    }

    @Override
    public Result<Void> removeUpVotePost(String postId, String userId, String userPassword) {
        return null;
    }

    @Override
    public Result<Void> downVotePost(String postId, String userId, String userPassword) {
        return null;
    }

    @Override
    public Result<Void> removeDownVotePost(String postId, String userId, String userPassword) {
        return null;
    }

    @Override
    public Result<Integer> getupVotes(String postId) {
        return null;
    }

    @Override
    public Result<Integer> getDownVotes(String postId) {
        return null;
    }

    @Override
    public Result<Void> updatePostOwner(String authorId, String password) {
        Invocation.Builder builder = target.path(authorId).path("posts")
                .queryParam(RestContent.PASSWORD, password).request();

        Response r = executeOperation(builder::delete);

        int status = r.getStatus();
        if (status == Response.Status.OK.getStatusCode()) {
            return Result.ok();
        } else {
            return Result.error(getErrorCodeFrom(status));
        }
    }

}
