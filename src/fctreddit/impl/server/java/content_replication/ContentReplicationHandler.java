package fctreddit.impl.server.java.content_replication;

import fctreddit.api.data.Post;
import fctreddit.api.java.Content;
import fctreddit.api.java.Result;
import fctreddit.impl.server.Hibernate;
import fctreddit.impl.server.java.ContentJava;
import fctreddit.impl.server.repl.KafkaPublisher;
import fctreddit.impl.server.repl.KafkaSubscriber;
import fctreddit.impl.server.repl.SyncPoint;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class ContentReplicationHandler implements Content {

    private static Map<String, Object> lockMap = new ConcurrentHashMap<>();

    Logger Log = Logger.getLogger(ContentJava.class.getName());
    private static final Object lock = new Object();
    private final Hibernate hibernate = Hibernate.getInstance();
    private static KafkaSubscriber subscriber;
    private static KafkaPublisher publisher;
    private final SyncPoint syncPoint = SyncPoint.getSyncPoint();

    public ContentReplicationHandler() {

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
        Log.info("Getting post " + postId + "\n");
        if (postId == null)
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        Post post;
        try {
            post = hibernate.get(Post.class, postId);
        } catch (Exception e) {
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        if (post == null)
            return Result.error(Result.ErrorCode.NOT_FOUND);
        return Result.ok(post);
    }

    @Override
    public Result<List<String>> getPostAnswers(String postId, long maxTimeout) {
                Log.info("Getting answers for post " + postId + "\n");
        List<Post> posts;
        if (maxTimeout != 0) {
            lockMap.putIfAbsent(postId, new Object());

            Object lock = lockMap.get(postId);
            synchronized (lock) {
                try {
                    lock.wait(maxTimeout);
                } catch (InterruptedException e) {
                    Log.severe(e.toString());
                    return Result.error(Result.ErrorCode.INTERNAL_ERROR);
                }
            }
        }
        try {
            posts = hibernate.jpql("SELECT p FROM Post p WHERE p.parentUrl LIKE '%" + postId + "' ORDER BY p.creationTimestamp", Post.class);
        } catch (Exception e) {
            Log.severe(e + "\n");
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        return Result.ok(posts.stream().map(Post::getPostId).toList());
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
        Post post;
        try {
            post = hibernate.get(Post.class, postId);
        } catch (Exception e) {
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        if (post == null)
            return Result.error(Result.ErrorCode.NOT_FOUND);

        return Result.ok(post.getUpVote());
    }

    @Override
    public Result<Integer> getDownVotes(String postId) {
                Post post;
        try {
            post = hibernate.get(Post.class, postId);
        } catch (Exception e) {
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        if (post == null)
            return Result.error(Result.ErrorCode.NOT_FOUND);
        return Result.ok(post.getDownVote());
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
