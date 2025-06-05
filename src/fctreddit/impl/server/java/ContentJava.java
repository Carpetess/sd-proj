package fctreddit.impl.server.java;

import fctreddit.api.data.Post;
import fctreddit.api.data.User;
import fctreddit.api.data.Vote;
import fctreddit.api.java.Content;
import fctreddit.api.java.Image;
import fctreddit.api.java.Result;
import fctreddit.api.java.Users;
import fctreddit.impl.server.Hibernate;
import fctreddit.impl.server.SecretKeeper;
import fctreddit.impl.server.kafka.KafkaPublisher;
import fctreddit.impl.server.kafka.KafkaSubscriber;
import fctreddit.impl.server.kafka.RecordProcessor;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;


public class ContentJava extends JavaServer implements Content {

    private static Map<String, Object> lockMap = new ConcurrentHashMap<>();

    private static final Logger Log = Logger.getLogger(ContentJava.class.getName());
    private Hibernate hibernate;
    private static KafkaPublisher publisher;


    public ContentJava() {
        hibernate = Hibernate.getInstance();
    }

    @Override
    public Result<String> createPost(Post post, String userPassword) {
        post.setPostId(UUID.randomUUID().toString());
        post.setCreationTimestamp(System.currentTimeMillis());
        if (!isValid(post))
            return Result.error(Result.ErrorCode.BAD_REQUEST);

        Result<User> user = getUser(post.getAuthorId(), userPassword);
        if (!user.isOK())
            return Result.error(user.error());
        Hibernate.TX tx = hibernate.beginTransaction();
        try {
            if (post.getParentUrl() != null && !post.getParentUrl().isBlank()) {
                String parentId = parseUrl(post.getParentUrl());
                if (hibernate.get(tx, Post.class, parentId) == null) {
                    hibernate.abortTransaction(tx);
                    return Result.error(Result.ErrorCode.NOT_FOUND);
                }

                lockMap.putIfAbsent(parentId, new Object());
                Object lock = lockMap.get(parentId);
                synchronized (lock) {
                    hibernate.persist(tx, post);
                    lock.notifyAll();
                }
                lockMap.remove(parentId);
            } else {
                hibernate.persist(tx, post);
            }
            hibernate.commitTransaction(tx);

            if (post.getMediaUrl() != null)
                changeReferenceOfImage(post, true);

        } catch (Exception e) {
            Log.severe(e.toString());
            hibernate.abortTransaction(tx);
            Log.warning("Broken when creating post: " + post.getPostId() + " " + e.getMessage() + "\n");
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        return Result.ok(post.getPostId());
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
            Log.warning("Broken when getting posts: " + e.getMessage() + "\n");
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
        else {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }
    }

    @Override
    public Result<List<String>> getPostAnswers(String postId, long maxTimeout) {
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
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        return Result.ok(posts.stream().map(Post::getPostId).toList());
    }

    @Override
    public Result<Post> updatePost(String postId, String userPassword, Post post) {
        Post oldPost = null;
        Hibernate.TX tx = hibernate.beginTransaction();
        try {
            oldPost = hibernate.get(tx, Post.class, postId);

            if (oldPost == null) {
                Log.severe("Post " + postId + " not found");
                hibernate.abortTransaction(tx);
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }

            List<Vote> votes = hibernate.jpql(tx, "SELECT v FROM Vote v WHERE v.postId LIKE '" + postId + "'", Vote.class);
            List<Post> comments = hibernate.jpql(tx, "SELECT p FROM Post p WHERE p.parentUrl LIKE '%" + postId + "'", Post.class);

            if (!comments.isEmpty() || !votes.isEmpty()) {
                hibernate.abortTransaction(tx);
                return Result.error(Result.ErrorCode.BAD_REQUEST);
            }

            if (post.getMediaUrl() != null) {
                if (oldPost.getMediaUrl() != null && !oldPost.getMediaUrl().equals(post.getMediaUrl())) {
                    changeReferenceOfImage(oldPost, false);
                    if (!oldPost.getMediaUrl().equals(post.getMediaUrl()))
                        changeReferenceOfImage(post, true);
                }

            }

            update(post, oldPost);
            Result<User> res = getUser(oldPost.getAuthorId(), userPassword);
            if (!res.isOK()) {
                hibernate.abortTransaction(tx);
                return Result.error(res.error());
            }
            hibernate.update(tx, oldPost);
            hibernate.commitTransaction(tx);
        } catch (Exception e) {
            Log.warning("Broken when updating post: " + postId + " " + e.getMessage() + "\n");
            hibernate.abortTransaction(tx);
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        Log.info("Updating post " + postId);
        return Result.ok(post);
    }

    @Override
    public Result<Void> deletePost(String postId, String userPassword) {
        Hibernate.TX tx = hibernate.beginTransaction();
        try {
            Users userClient = getUsersClient();
            Image imageClient = getImageClient();
            Post post = hibernate.get(tx, Post.class, postId);

            if (post == null) {
                hibernate.abortTransaction(tx);
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }

            Result<User> userRes = userClient.getUser(post.getAuthorId(), userPassword);
            if (!userRes.isOK()) {
                hibernate.abortTransaction(tx);
                return Result.error(Result.ErrorCode.FORBIDDEN);
            }
            List<Post> toDelete = deletePostHelper(tx, postId);
            toDelete.add(post);
            Log.info("Deleting " + toDelete.size() + " posts");
            hibernate.deleteAll(tx, toDelete);
            hibernate.commitTransaction(tx);
            if (post.getMediaUrl() != null)
                imageClient.deleteImage(post.getAuthorId(), parseUrl(post.getMediaUrl()), userPassword);
            for (Post deletedPost : toDelete) {
                if (deletedPost.getMediaUrl() != null)
                    changeReferenceOfImage(deletedPost, false);
                lockMap.remove(deletedPost.getPostId());
            }
        } catch (Exception e) {
            Log.warning("Broken when deleting post" + postId + " " + e.getMessage() + "\n");
            hibernate.abortTransaction(tx);
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }

        return Result.ok();
    }

    private List<Post> deletePostHelper(Hibernate.TX tx, String postId) {
        List<Post> answers = hibernate.jpql(tx, "SELECT p FROM Post p WHERE p.parentUrl LIKE '%" + postId + "'", Post.class);
        List<Post> toDelete = new LinkedList<>(answers);
        for (Post answer : answers) {
            toDelete.addAll(deletePostHelper(tx, answer.getPostId()));
        }
        return toDelete;
    }


    @Override
    public Result<Void> upVotePost(String postId, String userId, String userPassword) {
        Log.info("Executing upVote on " + postId + " with Userid:" + userId + " Password: " + userPassword);

        if (userPassword == null)
            return Result.error(Result.ErrorCode.FORBIDDEN);

        Result<User> u = this.getUsersClient().getUser(userId, userPassword);
        if (!u.isOK())
            return Result.error(u.error());

        Log.info("Retrieved user: " + u.value());

        Hibernate.TX tx = hibernate.beginTransaction();

        Post p = hibernate.get(tx, Post.class, postId);

        if (p == null) {
            hibernate.abortTransaction(tx);
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }
        try {
            hibernate.persist(tx, new Vote(userId, postId, true));
            hibernate.commitTransaction(tx);
            Log.info("Persisted vote");
        } catch (Exception e) {
            Log.warning("Broken when upvoting " + userId + " from post " + postId + " " + e.getMessage() + "\n");
            hibernate.abortTransaction(tx);
            return Result.error(Result.ErrorCode.CONFLICT);
        }

        return Result.ok();
    }

    @Override
    public Result<Void> removeUpVotePost(String postId, String userId, String userPassword) {
        Log.info("Executing removeUpVote on " + postId + " with Userid:" + userId + " Password: " + userPassword);

        if (userPassword == null)
            return Result.error(Result.ErrorCode.FORBIDDEN);

        Result<User> u = this.getUsersClient().getUser(userId, userPassword);
        if (!u.isOK())
            return Result.error(u.error());

        Log.info("Retrieved user: " + u.value());

        Hibernate.TX tx = hibernate.beginTransaction();

        List<Vote> i = hibernate.sql(tx, "SELECT * from Vote pv WHERE pv.userId='" + userId
                + "' AND pv.postId='" + postId + "' AND pv.upVote='true'", Vote.class);
        if (i.isEmpty()) {
            hibernate.abortTransaction(tx);
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }
        Post p = hibernate.get(tx, Post.class, postId);
        if (p == null) {
            hibernate.abortTransaction(tx);
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }
        try {
            hibernate.delete(tx, i.iterator().next());
            hibernate.commitTransaction(tx);
        } catch (Exception e) {
            Log.warning("Broken when removing upvote " + userId + " from post " + postId + " " + e.getMessage() + "\n");
            hibernate.abortTransaction(tx);
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }

        return Result.ok();
    }

    @Override
    public Result<Void> downVotePost(String postId, String userId, String userPassword) {
        Log.info("Executing downVote on " + postId + " with Userid:" + userId + " Password: " + userPassword);

        if (userPassword == null)
            return Result.error(Result.ErrorCode.FORBIDDEN);

        Result<User> u = this.getUsersClient().getUser(userId, userPassword);
        if (!u.isOK())
            return Result.error(u.error());

        Log.info("Retrieved user: " + u.value());

        Hibernate.TX tx = hibernate.beginTransaction();

        Post p = hibernate.get(tx, Post.class, postId);

        if (p == null) {
            hibernate.abortTransaction(tx);
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }
        Log.info("Updated post");
        try {
            hibernate.persist(tx, new Vote(userId, postId, false));
            hibernate.commitTransaction(tx);
        } catch (Exception e) {
            Log.warning("Broken when downvoting vote " + userId + " from post " + postId + " " + e.getMessage() + "\n");
            hibernate.abortTransaction(tx);
            return Result.error(Result.ErrorCode.CONFLICT);
        }

        return Result.ok();
    }

    @Override
    public Result<Void> removeDownVotePost(String postId, String userId, String userPassword) {
        Log.info("Executing removeDownVote on " + postId + " with Userid:" + userId + " Password: " + userPassword);

        if (userPassword == null)
            return Result.error(Result.ErrorCode.FORBIDDEN);

        Result<User> u = this.getUsersClient().getUser(userId, userPassword);
        if (!u.isOK())
            return Result.error(u.error());

        Log.info("Retrieved user: " + u.value());

        Hibernate.TX tx = hibernate.beginTransaction();

        List<Vote> i = hibernate.sql(tx, "SELECT * from Vote pv WHERE pv.userId='" + userId
                + "' AND pv.postId='" + postId + "' AND pv.upVote='false'", Vote.class);
        if (i.isEmpty()) {
            hibernate.abortTransaction(tx);
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }
        Post p = hibernate.get(tx, Post.class, postId);
        if (p == null) {
            hibernate.abortTransaction(tx);
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }

        try {
            hibernate.delete(tx, i.iterator().next());
            hibernate.commitTransaction(tx);
        } catch (Exception e) {
            Log.warning("Broken when removing down vote " + userId + " from post " + postId + " " + e.getMessage() + "\n");
            hibernate.abortTransaction(tx);
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }

        return Result.ok();
    }

    @Override
    public Result<Integer> getupVotes(String postId) {
        Post post;
        int upvotes;
        try {
            post = hibernate.get(Post.class, postId);
            upvotes = hibernate.sql("SELECT * from Vote pv WHERE pv.postId='" + postId + "' AND pv.upVote='true'", Vote.class).size();
        } catch (Exception e) {
            Log.warning("Broken when getting up votes for " + postId + ": " + e.getMessage() + "\n");
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        if (post == null)
            return Result.error(Result.ErrorCode.NOT_FOUND);

        return Result.ok(upvotes);
    }

    @Override
    public Result<Integer> getDownVotes(String postId) {
        Post post;
        int downvotes;
        try {
            post = hibernate.get(Post.class, postId);
            downvotes = hibernate.sql("SELECT * from Vote pv WHERE pv.postId='" + postId + "' AND pv.upVote='false'", Vote.class).size();
        } catch (Exception e) {
            Log.warning("Broken when getting down votes for " + postId + ": " + e.getMessage() + "\n");
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
        Log.info("Executing a removeTracesOfUser on " + userId);
        Hibernate.TX tx = hibernate.beginTransaction();
        try {

            hibernate.sql(tx, "DELETE from Vote v where v.voterId='" + userId + "'");

            hibernate.sql(tx, "UPDATE Post p SET p.authorId=NULL where p.authorId='" + userId + "'");

            hibernate.commitTransaction(tx);

        } catch (Exception e) {
            Log.warning("Broke, when removing user trace: " + e.getMessage() + "\n");
            hibernate.abortTransaction(tx);
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        return Result.ok();
    }


    private Result<User> getUser(String userId, String password) {
        Users userClient = getUsersClient();
        return userClient.getUser(userId, password);
    }

    private boolean isValid(Post post) {
        return post.getContent() != null && !post.getContent().isEmpty()
                && post.getAuthorId() != null && !post.getAuthorId().isBlank();
    }


    private void update(Post newPost, Post oldPost) {
        if (newPost.getContent() != null && !newPost.getContent().isBlank())
            oldPost.setContent(newPost.getContent());
        if (newPost.getMediaUrl() != null && !newPost.getMediaUrl().isBlank())
            oldPost.setMediaUrl(newPost.getMediaUrl());
    }

    private String parseUrl(String url) {
        String[] slice = url.split("/");
        String imageId = slice[slice.length - 1];
        return imageId;
    }

    private static void startSubscriber(KafkaSubscriber subscriber) {
        subscriber.start(new RecordProcessor() {
            Hibernate hibernate = Hibernate.getInstance();

            @Override
            public void onReceive(ConsumerRecord<String, String> r) {
                Hibernate.TX tx = hibernate.beginTransaction();
                String imageToRemove = r.value();
                hibernate.sql(tx, "UPDATE Post p SET p.mediaUrl=NULL WHERE p.mediaUrl LIKE '%" + imageToRemove + "'");

                hibernate.commitTransaction(tx);
            }
        });
    }

    public static void setPublisher(KafkaPublisher publisher) {
        ContentJava.publisher = publisher;
    }

    public static void setSubscriber(KafkaSubscriber subscriber) {
        startSubscriber(subscriber);
    }

    private void changeReferenceOfImage(Post post, boolean add) {
        String[] slice = post.getMediaUrl().split("/");
        String imageId = slice[slice.length - 1];
        String userId = slice[slice.length - 2];
        String message = post.getPostId() + " " + userId + "/" + imageId;
        if (add) {
            publisher.publish(Image.IMAGE_REFERENCE_COUNTER_TOPIC, "true", message);
        } else {
            publisher.publish(Image.IMAGE_REFERENCE_COUNTER_TOPIC, "false", message);
        }
    }

}
