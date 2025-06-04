package fctreddit.impl.server.rest;

import fctreddit.api.data.Post;
import fctreddit.api.java.Result;
import fctreddit.impl.server.java.ContentJava;
import fctreddit.api.rest.RestContent;
import fctreddit.impl.server.ErrorParser;
import jakarta.ws.rs.WebApplicationException;

import java.util.List;
import java.util.logging.Logger;

public class ContentResource  extends ErrorParser implements RestContent {
    private Logger Log = Logger.getLogger(String.valueOf(ContentResource.class));

    private ContentJava impl;
    public ContentResource() {
        impl = new ContentJava();
    }

    @Override
    public String createPost(Post post, String userPassword) {
        Log.info("createPost by user: " + userPassword);
        Result<String> res = impl.createPost(post, userPassword);

        if(!res.isOK()){
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
        return res.value();
    }

    @Override
    public List<String> getPosts(long timestamp, String sortOrder) {
        Log.info("getPosts by user: " + timestamp);
        Result<List<String>> res = impl.getPosts(timestamp, sortOrder);
        if(!res.isOK()){
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
        return res.value();
    }

    @Override
    public Post getPost(String postId) {
        Log.info("getPost by user: " + postId);
        Result<Post> res = impl.getPost(postId);
        if(!res.isOK()){
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
        return res.value();
    }

    @Override
    public List<String> getPostAnswers(String postId, long timeout) {
        Log.info("getPostAnswers by user: " + postId);
        Result<List<String>> res = impl.getPostAnswers(postId, timeout);
        if(!res.isOK()){
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
        return res.value();
    }

    @Override
    public Post updatePost(String postId, String userPassword, Post post) {
        Log.info("updatePost by user: " + postId);
        Result<Post> res = impl.updatePost(postId, userPassword, post);
        if(!res.isOK()){
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
        return res.value();
    }

    @Override
    public void deletePost(String postId, String userPassword) {
        Log.info("deletePost by user: " + postId);
        Result<Void> res = impl.deletePost(postId, userPassword);
        if(!res.isOK()){
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
    }

    @Override
    public void upVotePost(String postId, String userId, String userPassword) {
        Log.info("upVotePost by user: " + userId);
        Result<Void> res = impl.upVotePost(postId, userId, userPassword);
        if(!res.isOK()){
            throw new WebApplicationException(errorCodeToStatus(res.error()));

        }
    }

    @Override
    public void removeUpVotePost(String postId, String userId, String userPassword) {
        Log.info("removeUpVotePost by user: " + userId);
        Result<Void> res = impl.removeUpVotePost(postId, userId, userPassword);
        if(!res.isOK()){
            throw new WebApplicationException(errorCodeToStatus(res.error()));

        }

    }

    @Override
    public void downVotePost(String postId, String userId, String userPassword) {
        Log.info("downVotePost by user: " + userId);
        Result<Void> res = impl.downVotePost(postId, userId, userPassword);
        if(!res.isOK()){
            throw new WebApplicationException(errorCodeToStatus(res.error()));

        }
    }

    @Override
    public void removeDownVotePost(String postId, String userId, String userPassword) {
        Log.info("removeDownVotePost by user: " + userId);
        Result<Void> res = impl.removeDownVotePost(postId, userId, userPassword);
        if(!res.isOK()){
            throw new WebApplicationException(errorCodeToStatus(res.error()));

        }

    }

    @Override
    public Integer getupVotes(String postId) {
        Log.info("getupVotes by user: " + postId);
        Result<Integer> res = impl.getupVotes(postId);
        if(!res.isOK()){
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
        return res.value();
    }

    @Override
    public Integer getDownVotes(String postId) {
        Log.info("getDownVotes by user: " + postId);
        Result<Integer> res = impl.getDownVotes(postId);
        if(!res.isOK()){
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
        return res.value();
    }


    @Override
    public void removeUserTraces(String userId, String secret) {
        Log.info("updatePostOwner by user: " + userId);
        Result<Void> res = impl.removeUserTrace(userId,secret);
        if(!res.isOK()){
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
    }

}
