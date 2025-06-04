package fctreddit.impl.server.rest;

import fctreddit.api.data.Post;
import fctreddit.api.java.Content;
import fctreddit.api.java.Result;
import fctreddit.api.rest.RestContent;
import fctreddit.impl.server.java.contentReplication.ContentReplicaPre;
import fctreddit.impl.server.kafka.SyncPoint;
import fctreddit.impl.server.rest.filter.VersionFilter;
import jakarta.ws.rs.WebApplicationException;

import java.util.List;
import java.util.logging.Logger;

import static fctreddit.impl.server.rest.ErrorParser.errorCodeToStatus;

public class ContentReplResource implements RestContent {
    private static final Logger Log = Logger.getLogger(ContentReplResource.class.getName());

    private Content impl;
    private SyncPoint syncPoint;

    public ContentReplResource() {
        impl = new ContentReplicaPre();
        syncPoint = SyncPoint.getSyncPoint();
    }


    @Override
    public String createPost(Post post, String userPassword) {
        Log.info("createPost by user: " + post.getAuthorId());
        Result<String> res = impl.createPost(post, userPassword);
        Log.info(res.toString());
        if(!res.isOK()){
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
        return res.value();
    }

    @Override
    public List<String> getPosts(long timestamp, String sortOrder) {
        Long version = VersionFilter.version.get();
        syncPoint.waitForVersion(version);
        Result<List<String>> res = impl.getPosts(timestamp, sortOrder);

        if(!res.isOK()){
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
        return res.value();

    }

    @Override
    public Post getPost(String postId) {
        Long version = VersionFilter.version.get();
        syncPoint.waitForVersion(version);
        Result<Post> res = impl.getPost(postId);

        if(!res.isOK()){
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
        return res.value();
    }

    @Override
    public List<String> getPostAnswers(String postId, long maxTimeout) {
        Long version = VersionFilter.version.get();
        syncPoint.waitForVersion(version);
        Result<List<String>> res = impl.getPostAnswers(postId, maxTimeout);
        if(!res.isOK()){
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }

        return res.value();
    }

    @Override
    public Post updatePost(String postId, String userPassword, Post post) {
        Result<Post> res = impl.updatePost(postId, userPassword, post);
        if(!res.isOK()){
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
        return res.value();
    }

    @Override
    public void deletePost(String postId, String userPassword) {
        Result<Void> res = impl.deletePost(postId, userPassword);
        if(!res.isOK()){
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
    }

    @Override
    public void upVotePost(String postId, String userId, String userPassword) {
        Result<Void> res = impl.upVotePost(postId, userId, userPassword);
        if(!res.isOK()){
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
    }

    @Override
    public void removeUpVotePost(String postId, String userId, String userPassword) {
        Result<Void> res = impl.removeUpVotePost(postId, userId, userPassword);
        if(!res.isOK()){
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
    }

    @Override
    public void downVotePost(String postId, String userId, String userPassword) {
        Result<Void> res = impl.downVotePost(postId, userId, userPassword);
        if(!res.isOK()){
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
    }

    @Override
    public void removeDownVotePost(String postId, String userId, String userPassword) {
        Result<Void> res = impl.removeDownVotePost(postId, userId, userPassword);
        if(!res.isOK()){
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
    }

    @Override
    public Integer getupVotes(String postId) {
        Long version = VersionFilter.version.get();
        syncPoint.waitForVersion(version);
        Result<Integer> res = impl.getupVotes(postId);
        if(!res.isOK()){
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
        return res.value();
    }

    @Override
    public Integer getDownVotes(String postId) {
        Long version = VersionFilter.version.get();
        syncPoint.waitForVersion(version);
        Result<Integer> res = impl.getDownVotes(postId);
        if(!res.isOK()){
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
        return res.value();
    }

    @Override
    public void removeUserTraces(String userId, String secret) {
        Result<Void> res = impl.removeUserTrace(userId, secret);
        if(!res.isOK()){
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
    }

}
