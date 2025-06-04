package fctreddit.impl.server.java.contentReplication;

import com.google.gson.Gson;
import fctreddit.api.data.Post;
import fctreddit.api.data.User;
import fctreddit.api.data.Vote;
import fctreddit.api.java.Content;
import fctreddit.api.java.Result;
import fctreddit.api.java.Users;
import fctreddit.impl.server.Hibernate;
import fctreddit.impl.server.SecretKeeper;
import fctreddit.impl.server.java.JavaServer;
import fctreddit.impl.server.kafka.KafkaPublisher;
import fctreddit.impl.server.kafka.SyncPoint;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class ContentReplicaPre extends JavaServer implements Content {


    private static final Logger Log = Logger.getLogger(ContentReplicaPre.class.getName());

    private static KafkaPublisher kafkaPublisher;
    private static final Gson gson = new Gson();

    private Hibernate hibernate;
    private SyncPoint syncPoint;

    public ContentReplicaPre() {
        hibernate = Hibernate.getInstance();
        syncPoint = SyncPoint.getSyncPoint();
    }

    @Override
    public Result<String> createPost(Post post, String userPassword) {

        if (!isValid(post))
            return Result.error(Result.ErrorCode.BAD_REQUEST);

        Result<User> user = getUser(post.getAuthorId(), userPassword);
        if (!user.isOK())
            return Result.error(user.error());

        post.setPostId(UUID.randomUUID().toString());
        post.setCreationTimestamp(System.currentTimeMillis());

        String postJson = gson.toJson(post);
        long offset = publishOperationToKafka(CREATE_POST, postJson, userPassword);
        Result<?> res = syncPoint.waitForResult(offset);
        if (res.isOK()) {
            return Result.ok((String) res.value());
        }
        return Result.error(res.error());
    }

    @Override
    public Result<List<String>> getPosts(long timestamp, String sortOrder) {
        if (sortOrder == null) {
            sortOrder = "";
        }

        String commentCountQuery = "(SELECT COUNT(r) FROM Post r WHERE r.parentUrl LIKE CONCAT('%', p.postId))";

        List<String> posts;
        try {
            switch (sortOrder) {
                case MOST_UP_VOTES -> posts = hibernate.sql("SELECT postId FROM (SELECT p.postId as postId, "
                        + "(SELECT COUNT(*) FROM Vote pv where p.postId = pv.postId AND pv.upVote='true') as upVotes "
                        + "from Post p WHERE "
                        + (timestamp > 0 ? "p.creationTimestamp >= '" + timestamp + "' AND " : "")
                        + "p.parentURL IS NULL) ORDER BY upVotes DESC, postID ASC", String.class);
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
        Post p = hibernate.get(Post.class, postId);
        Result<Integer> res = this.getupVotes(postId);
        if (res.isOK())
            p.setUpVote(res.value());
        res = this.getDownVotes(postId);
        if (res.isOK())
            p.setDownVote(res.value());
        if (p != null)
            return Result.ok(p);
        else
            return Result.error(Result.ErrorCode.NOT_FOUND);
    }

    @Override
    public Result<List<String>> getPostAnswers(String postId, long maxTimeout) {
        return null;
    }

    @Override
    public Result<Post> updatePost(String postId, String userPassword, Post post) {
        if (post == null) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
        String postJson = gson.toJson(post);
        long offset = publishOperationToKafka(UPDATE_POST, postId, userPassword, postJson);
        Result<?> res = syncPoint.waitForResult(offset);
        if (res.isOK()) {
            return Result.ok((Post) res.value());
        }
        return Result.error(res.error());
    }

    @Override
    public Result<Void> deletePost(String postId, String userPassword) {
        if (postId == null) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
        long offset = publishOperationToKafka(DELETE_POST, postId, userPassword);
        Result<?> res = syncPoint.waitForResult(offset);
        if (res.isOK()) {
            return Result.ok();
        }
        return Result.error(res.error());

    }

    @Override
    public Result<Void> upVotePost(String postId, String userId, String userPassword) {
        Result<User> userRes = getUser(userId, userPassword);
        if (!userRes.isOK()) {
            return Result.error(userRes.error());
        }
        if (postId == null) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
        long offset = publishOperationToKafka(UPVOTE_POST, postId, userPassword);
        Result<?> res = syncPoint.waitForResult(offset);
        if (res.isOK()) {
            return Result.ok();
        }
        return Result.error(res.error());

    }

    @Override
    public Result<Void> removeUpVotePost(String postId, String userId, String userPassword) {
        Result<User> userRes = getUser(userId, userPassword);
        if (!userRes.isOK()) {
            return Result.error(userRes.error());
        }
        if (postId == null) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
        long offset = publishOperationToKafka(REMOVE_UPVOTE_POST, postId, userPassword);
        Result<?> res = syncPoint.waitForResult(offset);
        if (res.isOK()) {
            return Result.ok();
        }
        return Result.error(res.error());
    }

    @Override
    public Result<Void> downVotePost(String postId, String userId, String userPassword) {
        Result<User> userRes = getUser(userId, userPassword);
        if (!userRes.isOK()) {
            return Result.error(userRes.error());
        }
        if (postId == null) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
        long offset = publishOperationToKafka(DOWNVOTE_POST, postId, userPassword);
        Result<?> res = syncPoint.waitForResult(offset);
        if (res.isOK()) {
            return Result.ok();
        }
        return Result.error(res.error());
    }

    @Override
    public Result<Void> removeDownVotePost(String postId, String userId, String userPassword) {
        Result<User> userRes = getUser(userId, userPassword);
        if (!userRes.isOK()) {
            return Result.error(userRes.error());
        }
        if (postId == null) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
        long offset = publishOperationToKafka(REMOVE_DOWNVOTE_POST, postId, userPassword);
        Result<?> res = syncPoint.waitForResult(offset);
        if (res.isOK()) {
            return Result.ok();
        }
        return Result.error(res.error());
    }

    @Override
    public Result<Integer> getupVotes(String postId) {
        Post post;
        int downvotes;
        try {
            post = hibernate.get(Post.class, postId);
            downvotes = hibernate.sql("SELECT * from Vote pv WHERE pv.postId='" + postId + "' AND pv.upVote='true'", Vote.class).size();
        } catch (Exception e) {
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        if (post == null)
            return Result.error(Result.ErrorCode.NOT_FOUND);
        return Result.ok(downvotes);
    }

    @Override
    public Result<Integer> getDownVotes(String postId) {
        Post post;
        int downvotes;
        try {
            post = hibernate.get(Post.class, postId);
            downvotes = hibernate.sql("SELECT * from Vote pv WHERE pv.postId='" + postId + "' AND pv.upVote='false'", Vote.class).size();
        } catch (Exception e) {
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        if (post == null)
            return Result.error(Result.ErrorCode.NOT_FOUND);
        return Result.ok(downvotes);
    }

    @Override
    public Result<Void> removeUserTrace(String userId, String secret) {
        if (!secret.equals(SecretKeeper.getInstance().getSecret())) {
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }
        long offset = publishOperationToKafka(REMOVE_USER_TRACE, userId, secret);
        Result<?> res = syncPoint.waitForResult(offset);
        if (res.isOK()) {
            return Result.ok();
        }
        return Result.error(res.error());
    }

    private long publishOperationToKafka(String operation, String... parameters) {
        String parametersString;
        if (parameters.length > 1) {
            parametersString = StringUtils.join(parameters, "\t");
        } else {
            parametersString = parameters[0];
        }
        return kafkaPublisher.publish(SEND_OPERATION, operation, parametersString);
    }

    private static void setKafkaPublisher(KafkaPublisher publisher) {
        kafkaPublisher = publisher;
    }

    private Result<User> getUser(String userId, String password) {
        Users userClient = getUsersClient();
        return userClient.getUser(userId, password);
    }

    private boolean isValid(Post post) {
        return post.getContent() != null && !post.getContent().isEmpty()
                && post.getAuthorId() != null && !post.getAuthorId().isBlank();
    }

    private String parseUrl(String url) {
        String[] slice = url.split("/");
        String imageId = slice[slice.length - 1];
        return imageId;
    }
}
