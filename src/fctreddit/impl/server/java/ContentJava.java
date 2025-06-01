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
import fctreddit.impl.server.repl.KafkaPublisher;
import fctreddit.impl.server.repl.KafkaSubscriber;
import fctreddit.impl.server.repl.KafkaUtils;
import fctreddit.impl.server.repl.RecordProcessor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.internals.events.ListOffsetsEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;


public class ContentJava extends JavaServer implements Content {


    private static Map<String, Object> lockMap = new ConcurrentHashMap<>();

    Logger Log = Logger.getLogger(ContentJava.class.getName());
    private static final Object lock = new Object();
    private final Hibernate hibernate;
    private static KafkaSubscriber subscriber;
    private static KafkaPublisher publisher;

    public ContentJava() {
        Log.info("Starting ContentJava");
        synchronized (lock){
            if (subscriber == null) {
                KafkaUtils.createTopic(Image.DELETED_IMAGE_TOPIC);
                subscriber = KafkaSubscriber.createSubscriber("kafka:9092", List.of(Image.DELETED_IMAGE_TOPIC));
                startSubscriber(subscriber);
            }
            if (publisher == null) {
                KafkaUtils.createTopic(Image.REFERENCE_COUNTER_TOPIC);
                publisher = KafkaPublisher.createPublisher("kafka:9092");
            }
        }
        Log.info("Finished ContentJava");
        hibernate = Hibernate.getInstance();
    }

    @Override
    public Result<String> createPost(Post post, String userPassword) {
        Log.info("Creating post " + post.getPostId() + "\n");
        post.setPostId(UUID.randomUUID().toString());
        post.setCreationTimestamp(System.currentTimeMillis());
        if (post.getMediaUrl() != null && !post.getMediaUrl().isBlank())
            changeReferenceOfImage(post.getMediaUrl(), true);
        // Enviar para as replicas
        return createPostGeneric(post, userPassword);
    }

    private Result<String> createPostGeneric(Post post, String userPassword) {
        Log.info("Writing the post to the database" + post.getPostId() + "\n");
        if (!isValid(post))
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        Result<User> user = getUser(post.getAuthorId(), userPassword);
        if (!user.isOK())
            return Result.error(user.error());
        try {
            if (post.getParentUrl() != null && !post.getParentUrl().isBlank()) {
                String parentId = parseUrl(post.getParentUrl());
                if (hibernate.get(Post.class, parentId) == null)
                    return Result.error(Result.ErrorCode.NOT_FOUND);

                lockMap.putIfAbsent(parentId, new Object());
                Object lock = lockMap.get(parentId);
                synchronized (lock) {
                    hibernate.persist(post);
                    lock.notifyAll();
                }
                lockMap.remove(parentId);
            } else {
                hibernate.persist(post);
            }

        } catch (Exception e) {
            Log.severe(e + "\n");
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        return Result.ok(post.getPostId());
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
        Log.info("Updating post " + postId + "\n");
        Post oldPost = null;
        try {
            oldPost = hibernate.get(Post.class, postId);

            if (oldPost == null) {
                Log.severe("Post " + postId + " not found" + "\n");
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }

            List<Vote> votes = hibernate.jpql("SELECT v FROM Vote v WHERE v.postId LIKE '" + postId + "'", Vote.class);
            List<Post> comments = hibernate.jpql("SELECT p FROM Post p WHERE p.parentUrl LIKE '%" + postId + "'", Post.class);

            if (!comments.isEmpty() || !votes.isEmpty())
                return Result.error(Result.ErrorCode.BAD_REQUEST);

            if (post.getMediaUrl() != null && !post.getMediaUrl().isBlank() && (oldPost.getMediaUrl() == null || !oldPost.getMediaUrl().equals(post.getMediaUrl()))) {
                Log.info("Changing reference of image " + oldPost.getMediaUrl() + " to " + post.getMediaUrl() + "\n");
                if (oldPost.getMediaUrl() != null && !oldPost.getMediaUrl().isBlank())
                    changeReferenceOfImage(oldPost.getMediaUrl(), false);
                changeReferenceOfImage(post.getMediaUrl(), true);
            }

            update(post, oldPost);
            Result<User> res = getUser(oldPost.getAuthorId(), userPassword);
            if (!res.isOK())
                return Result.error(res.error());
            hibernate.update(oldPost);
        } catch (Exception e) {
            Log.severe(e + "\n");
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        Log.info("Updating post " + postId);
        return Result.ok(post);
    }

    @Override
    public Result<Void> deletePost(String postId, String userPassword) {
        Log.info("Deleting post " + postId + "\n");
        try {
            Users userClient = getUsersClient();
            Image imageClient = getImageClient();
            Post post = hibernate.get(Post.class, postId);

            if (post == null)
                return Result.error(Result.ErrorCode.NOT_FOUND);

            Result<User> userRes = userClient.getUser(post.getAuthorId(), userPassword);
            if (!userRes.isOK())
                return Result.error(Result.ErrorCode.FORBIDDEN);
            List<Post> toDelete = deletePostHelper(postId);
            if (!toDelete.isEmpty()) {
                for (Post p : toDelete) {
                    if (p.getMediaUrl() != null && !p.getMediaUrl().isBlank())
                        changeReferenceOfImage(p.getMediaUrl(), false);
                }
            }
            toDelete.add(post);
            Log.info("Deleting " + toDelete.size() + " posts");
            hibernate.deleteAll(toDelete);
            if (post.getMediaUrl() != null && !post.getMediaUrl().isBlank())
                imageClient.deleteImage(post.getAuthorId(), parseUrl(post.getMediaUrl()), userPassword);

        } catch (Exception e) {
            Log.severe(e + "\n");
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }

        return Result.ok();
    }

    private List<Post> deletePostHelper(String postId) {
        List<Post> answers = hibernate.jpql("SELECT p FROM Post p WHERE p.parentUrl LIKE '%" + postId + "'", Post.class);
        List<Post> toDelete = new LinkedList<>(answers);
        for (Post answer : answers) {
            toDelete.addAll(deletePostHelper(answer.getPostId()));
        }
        return toDelete;
    }


    @Override
    public Result<Void> upVotePost(String postId, String userId, String userPassword) {
        Log.info("Executing upVote on " + postId + " with Userid:" + userId + " Password: " + userPassword + "\n");

        if (userPassword == null)
            return Result.error(Result.ErrorCode.FORBIDDEN);

        Result<User> u = this.getUsersClient().getUser(userId, userPassword);
        if (!u.isOK())
            return Result.error(u.error());

        Log.info("Retrieved user: " + u.value() + "\n");

        Hibernate.TX tx = hibernate.beginTransaction();

        Post p = hibernate.get(tx, Post.class, postId);

        if (p == null) {
            hibernate.abortTransaction(tx);
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }
        p.setUpVote(p.getUpVote() + 1);
        try {
            hibernate.persistVote(tx, new Vote(userId, postId, true), p);
            Log.info("Persisted upvote " + userId + " " + postId + "\n");
        } catch (Exception e) {
            hibernate.abortTransaction(tx);
            return Result.error(Result.ErrorCode.CONFLICT);
        }
        return Result.ok();
    }

    @Override
    public Result<Void> removeUpVotePost(String postId, String userId, String userPassword) {
        Log.info("Executing removeUpVote on " + postId + " with Userid:" + userId + " Password: " + userPassword + "\n");

        if (userPassword == null)
            return Result.error(Result.ErrorCode.FORBIDDEN);

        Result<User> u = this.getUsersClient().getUser(userId, userPassword);
        if (!u.isOK())
            return Result.error(u.error());

        Log.info("Retrieved user: " + u.value() + "\n");

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
        p.setUpVote(p.getUpVote() - 1);

        try {
            hibernate.deleteVote(tx, i.iterator().next(), p);
            Log.info("Removing upvote " + userId + " " + postId + "\n");
        } catch (Exception e) {
            hibernate.abortTransaction(tx);
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }

        return Result.ok();
    }

    @Override
    public Result<Void> downVotePost(String postId, String userId, String userPassword) {
        Log.info("Executing downVote on " + postId + " with Userid:" + userId + " Password: " + userPassword + "\n");

        if (userPassword == null)
            return Result.error(Result.ErrorCode.FORBIDDEN);

        Result<User> u = this.getUsersClient().getUser(userId, userPassword);
        if (!u.isOK())
            return Result.error(u.error());

        Log.info("Retrieved user: " + u.value() + "\n");

        Hibernate.TX tx = hibernate.beginTransaction();

        Post p = hibernate.get(tx, Post.class, postId);

        if (p == null) {
            Log.severe("Post " + postId + " not found" + "\n");
            hibernate.abortTransaction(tx);
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }
        p.setDownVote(p.getDownVote() + 1);

        try {
            hibernate.persistVote(tx, new Vote(userId, postId, false), p);
            Log.info("Downvoted post " + postId + "\n");
        } catch (Exception e) {
            Log.severe(e + "\n");
            hibernate.abortTransaction(tx);
            return Result.error(Result.ErrorCode.CONFLICT);
        }

        return Result.ok();
    }

    @Override
    public Result<Void> removeDownVotePost(String postId, String userId, String userPassword) {
        Log.info("Executing removeDownVote on " + postId + " with Userid:" + userId + " Password: " + userPassword + "\n");

        if (userPassword == null)
            return Result.error(Result.ErrorCode.FORBIDDEN);

        Result<User> u = this.getUsersClient().getUser(userId, userPassword);
        if (!u.isOK())
            return Result.error(u.error());

        Log.info("Retrieved user: " + u.value() + "\n");

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
        p.setDownVote(p.getDownVote() - 1);

        try {
            hibernate.deleteVote(tx, i.iterator().next(), p);
            Log.info("Removed downvote " + userId + " " + postId + "\n");
        } catch (Exception e) {
            hibernate.abortTransaction(tx);
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }

        return Result.ok();
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
        if (!secret.equals(SecretKeeper.getInstance().getSecret())) {
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }
        Log.info("Updating owner for user " + userId + "'s posts");
        Result<User> res = getUser(userId, password);
        if (!res.isOK())
            return Result.error(res.error());
        try {
            List<Post> posts = hibernate.jpql("SELECT p FROM Post p WHERE p.authorId LIKE '" + userId + "'", Post.class);
            for (Post post : posts) {
                post.setAuthorId(null);
            }
            hibernate.updateAll(posts);
        } catch (Exception e) {
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        return Result.ok();
    }

    @Override
    public Result<Void> removeAllUserVotes(String userId, String password, String secret) {
        Hibernate.TX tx = hibernate.beginTransaction();
        Log.info("Removing all votes for user " + userId + "with secret: " + secret + " and this is my secret: "
                + SecretKeeper.getInstance().getSecret() + "\n");
        if (!secret.equals(SecretKeeper.getInstance().getSecret())) {
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }
        Log.info("Removing all votes for user " + userId);
        Result<User> res = getUser(userId, password);
        if (!res.isOK())
            return Result.error(res.error());
        try {
            List<Vote> votes = hibernate.jpql(tx, "SELECT v FROM Vote v WHERE v.voterId LIKE '" + userId + "'", Vote.class);
            List<Post> posts = new LinkedList<>();
            for (Vote vote : votes) {
                Post post = hibernate.get(tx, Post.class, vote.getPostId());
                if (vote.isUpVote())
                    post.setUpVote(post.getUpVote() - 1);

                if (!vote.isUpVote())
                    post.setDownVote(post.getDownVote() - 1);
                posts.add(post);
            }
            hibernate.updateAll(posts);
            hibernate.deleteAll(votes);
        } catch (Exception e) {
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        } finally {
            hibernate.commitTransaction(tx);
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

    private void startSubscriber(KafkaSubscriber subscriber) {
        subscriber.start(new RecordProcessor() {
            @Override
            public void onReceive(ConsumerRecord<String, String> r) {
                Hibernate.TX tx = hibernate.beginTransaction();
                List<Post> posts = hibernate.jpql(tx, "SELECT p FROM Post p WHERE p.mediaUrl LIKE '%" + r.value() + "'", Post.class);
                for (Post post : posts) {
                    post.setMediaUrl(null);
                }
                hibernate.updateAll(tx, posts);
                hibernate.commitTransaction(tx);
            }
        });
    }

    private void changeReferenceOfImage(String imageURI, boolean addReference) {
        String[] parts = imageURI.split("/");
        String imageId = parts[parts.length - 1];
        String userId = parts[parts.length - 2];
        String message = userId + "/" + imageId + " " + (addReference ? Image.ADD_IMAGE : Image.REMOVE_IMAGE);
        publisher.publish(Image.REFERENCE_COUNTER_TOPIC, message);
    }
}
