package fctreddit.impl.server.java.contentReplication;

import fctreddit.api.data.Post;
import fctreddit.api.java.Content;
import fctreddit.api.java.Result;
import fctreddit.impl.server.Hibernate;
import fctreddit.impl.server.java.JavaServer;
import fctreddit.impl.server.kafka.KafkaPublisher;
import fctreddit.impl.server.kafka.KafkaSubscriber;
import fctreddit.impl.server.kafka.SyncPoint;

import java.util.List;

public class ContentReplicaProcessor extends JavaServer implements Content {
    private Hibernate hibernate;
    private SyncPoint syncPoint;

    private static KafkaPublisher kafkaPublisher;
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
    public Result<Void> removeUserTrace(String userId, String secret) {
        return null;
    }



    private static void setPublisher(KafkaPublisher publisher) {
        kafkaPublisher = publisher;
    }

}