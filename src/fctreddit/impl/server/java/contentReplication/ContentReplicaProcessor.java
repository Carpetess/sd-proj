package fctreddit.impl.server.java.contentReplication;

import fctreddit.api.data.Post;
import fctreddit.api.data.User;
import fctreddit.api.data.Vote;
import fctreddit.api.java.Content;
import fctreddit.api.java.Image;
import fctreddit.api.java.Result;
import fctreddit.api.java.Users;
import fctreddit.impl.server.Hibernate;
import fctreddit.impl.server.SecretKeeper;
import fctreddit.impl.server.java.JavaServer;
import fctreddit.impl.server.kafka.KafkaPublisher;
import fctreddit.impl.server.kafka.KafkaSubscriber;
import fctreddit.impl.server.kafka.RecordProcessor;
import fctreddit.impl.server.kafka.SyncPoint;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class ContentReplicaProcessor extends JavaServer implements Content {
    private static final Logger Log = Logger.getLogger(ContentReplicaProcessor.class.getName());
    private static Map<String, Object> lockMap = new ConcurrentHashMap<>();
    private static KafkaPublisher kafkaPublisher;

    private Hibernate hibernate;

    public ContentReplicaProcessor() {
        hibernate = Hibernate.getInstance();
    }

    @Override
    public Result<String> createPost(Post post, String userPassword) {
        Log.info("Creating Post :" + post.getPostId() + " for user: " + post.getAuthorId() + "\n");
        Hibernate.TX tx = hibernate.beginTransaction();
        try {
            if (post.getParentUrl() != null && !post.getParentUrl().isBlank()) {
                String parentId = parseUrl(post.getParentUrl());
                if (hibernate.get(tx, Post.class, parentId) == null)
                    return Result.error(Result.ErrorCode.NOT_FOUND);

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
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        Log.info("Created Post :" + post.getPostId() + "\n");
        return Result.ok(post.getPostId());
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
        Post oldPost = null;
        try {
            oldPost = hibernate.get(Post.class, postId);

            if (oldPost == null) {
                Log.severe("Post " + postId + " not found");
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }

            List<Vote> votes = hibernate.jpql("SELECT v FROM Vote v WHERE v.postId LIKE '" + postId + "'", Vote.class);
            List<Post> comments = hibernate.jpql("SELECT p FROM Post p WHERE p.parentUrl LIKE '%" + postId + "'", Post.class);

            if (!comments.isEmpty() || !votes.isEmpty())
                return Result.error(Result.ErrorCode.BAD_REQUEST);

            if (post.getMediaUrl() != null) {
                if (oldPost.getMediaUrl() != null) {
                    changeReferenceOfImage(oldPost, false);
                }
                changeReferenceOfImage(post, true);
            }

            update(post, oldPost);
            Result<User> res = getUser(oldPost.getAuthorId(), userPassword);
            if (!res.isOK())
                return Result.error(res.error());
            hibernate.update(oldPost);

        } catch (Exception e) {
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        Log.info("Updating post " + postId);
        return Result.ok(post);
    }

    @Override
    public Result<Void> deletePost(String postId, String userPassword) {
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
            toDelete.add(post);
            Log.info("Deleting " + toDelete.size() + " posts");
            hibernate.deleteAll(toDelete);
            if (post.getMediaUrl() != null)
                imageClient.deleteImage(post.getAuthorId(), parseUrl(post.getMediaUrl()), userPassword);
            for (Post deletedPost : toDelete) {
                changeReferenceOfImage(deletedPost, false);
            }
        } catch (Exception e) {
            Log.severe(e.toString());
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
            hibernate.abortTransaction(tx);
            return Result.error(Result.ErrorCode.CONFLICT);
        }

        return Result.ok();
    }

    @Override
    public Result<Void> removeUpVotePost(String postId, String userId, String userPassword) {

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
            hibernate.abortTransaction(tx);
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }

        return Result.ok();
    }

    @Override
    public Result<Void> downVotePost(String postId, String userId, String userPassword) {

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
            hibernate.abortTransaction(tx);
            return Result.error(Result.ErrorCode.CONFLICT);
        }

        return Result.ok();
    }

    @Override
    public Result<Void> removeDownVotePost(String postId, String userId, String userPassword) {

        Log.info("Executing a removeTracesOfUser on " + userId);
        Hibernate.TX tx = null;
        try {
            tx = hibernate.beginTransaction();

            hibernate.sql(tx, "DELETE from Vote v where v.userId='" + userId + "'");

            hibernate.sql(tx, "UPDATE Post p SET p.authorId=NULL where p.authorId='" + userId + "'");

            hibernate.commitTransaction(tx);

        } catch (Exception e) {
            e.printStackTrace();
            hibernate.abortTransaction(tx);
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        return Result.ok();
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
        if (!secret.equals(SecretKeeper.getInstance().getSecret())) {
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }
        Log.info("Executing a removeTracesOfUser on " + userId);
        Hibernate.TX tx = null;
        try {
            tx = hibernate.beginTransaction();

            hibernate.sql(tx, "DELETE from Vote v where v.voterId='" + userId + "'");

            hibernate.sql(tx, "UPDATE Post p SET p.authorId=NULL where p.authorId='" + userId + "'");

            hibernate.commitTransaction(tx);

        } catch (Exception e) {
            e.printStackTrace();
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

    private void changeReferenceOfImage(Post post, boolean add) {
        String[] slice = post.getMediaUrl().split("/");
        String imageId = slice[slice.length - 1];
        String userId = slice[slice.length - 2];
        String message = post.getPostId() + " " + userId + "/" + imageId;
        if (add) {
            kafkaPublisher.publish(Image.IMAGE_REFERENCE_COUNTER_TOPIC, "true", message);
        } else {
            kafkaPublisher.publish(Image.IMAGE_REFERENCE_COUNTER_TOPIC, "false", message);
        }
    }

    public static void setKafkaPublisher(KafkaPublisher publisher) {
        kafkaPublisher = publisher;
    }

    public static void startSubscriber(KafkaSubscriber subscriber) {
        subscriber.start(new RecordProcessor() {
            Hibernate hibernate = Hibernate.getInstance();

            @Override
            public void onReceive(ConsumerRecord<String, String> r) {
                Hibernate.TX tx = hibernate.beginTransaction();
                String imageToRemove = r.value();
                List<Post> posts = hibernate.jpql(tx, "SELECT p FROM Post p WHERE p.mediaUrl LIKE '%" + imageToRemove + "'", Post.class);
                for (Post post : posts) {
                    post.setMediaUrl(null);
                }
                hibernate.updateAll(tx, posts);
                hibernate.commitTransaction(tx);
            }
        });
    }

    public static Map<String, Object> getLockMap() {
        return lockMap;
    }

}
