package fctreddit.impl.server.java.content_replication;

import fctreddit.api.data.Post;
import fctreddit.api.java.Content;
import fctreddit.api.java.Result;
import fctreddit.impl.server.Hibernate;
import fctreddit.impl.server.repl.SyncPoint;

import java.util.List;
import java.util.logging.Logger;

public class ContentReplicationHandler implements Content {

    private Hibernate hibernate;
    private static final Logger Log = Logger.getLogger(ContentReplicationHandler.class.getName());
    private static SyncPoint syncPoint;
    public ContentReplicationHandler() {
        syncPoint = SyncPoint.getSyncPoint();
        Hibernate.getInstance();
    }

    @Override
    public Result<String> createPost(Post post, String userPassword) {
        return null;
    }

    @Override
    public Result<List<String>> getPosts(long timestamp, String sortOrder) {
        Log.info("Getting posts since " + timestamp + "\n");
        if (sortOrder == null) {
            sortOrder = "";
        }
        String commentCountQuery = "(SELECT COUNT(r) FROM Post r WHERE r.parentUrl LIKE CONCAT('%', p.postId))";
        List<String> posts;
        try {
            switch (sortOrder) {
                case MOST_UP_VOTES ->
                        posts = hibernate.jpql("SELECT p.postId FROM Post p WHERE p.creationTimestamp >= " + timestamp + " AND p.parentUrl IS NULL ORDER BY p.upVote DESC, p.postId ASC", String.class);
                case MOST_REPLIES -> {
                    posts = hibernate.jpql("SELECT p.postId FROM Post p WHERE p.creationTimestamp >= " + timestamp + " AND p.parentUrl IS NULL ORDER BY " + commentCountQuery + " DESC,  p.postId ASC", String.class);
                }
                default ->
                        posts = hibernate.jpql("SELECT p.postId FROM Post p WHERE p.creationTimestamp >= " + timestamp + " AND p.parentUrl IS NULL ORDER BY p.creationTimestamp ASC", String.class);
            }
        } catch (Exception e) {
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        Log.info("Found " + posts.size() + " posts");
        return Result.ok(posts);
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
    public Result<Void> updatePostOwner(String userId, String password, String secret) {
        return null;
    }

    @Override
    public Result<Void> removeAllUserVotes(String userId, String password, String secret) {
        return null;
    }
}
