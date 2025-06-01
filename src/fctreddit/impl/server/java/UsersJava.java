package fctreddit.impl.server.java;

import fctreddit.api.data.User;
import fctreddit.api.java.Content;
import fctreddit.api.java.Image;
import fctreddit.api.java.Result;
import fctreddit.api.java.Users;
import fctreddit.impl.client.UsersClient;
import fctreddit.impl.server.Hibernate;
import fctreddit.impl.server.SecretKeeper;
import fctreddit.impl.server.repl.KafkaPublisher;
import fctreddit.impl.server.repl.KafkaSubscriber;
import fctreddit.impl.server.repl.KafkaUtils;
import fctreddit.impl.server.repl.RecordProcessor;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

public class UsersJava extends JavaServer implements Users {

    private static Logger Log = Logger.getLogger(UsersJava.class.getName());
    private final Hibernate hibernate;
    private final static Object lock = new Object();
    private static KafkaSubscriber subscriber;
    private static KafkaPublisher publisher;

    public UsersJava() {
        Log.info("Starting UsersJava");
            synchronized (lock) {
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
        Log.info("Finished UsersJava");
        hibernate = Hibernate.getInstance();
    }

    @Override
    public Result<String> createUser(User user) {
        Log.info("Creating user " + user.getUserId() + "\n");
        Hibernate hibernate = Hibernate.getInstance();
        try {
            if (!user.canBuild()) {
                return Result.error(Result.ErrorCode.BAD_REQUEST);
            }
            if (hibernate.get(User.class, user.getUserId()) != null) {
                return Result.error(Result.ErrorCode.CONFLICT);
            }
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty())
                changeReferenceOfImage(user.getAvatarUrl(), true);
            hibernate.persist(user);
        } catch (Exception e) {
            Log.severe(e + "Exception persisting user " + user.getUserId() + "\n");
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        return Result.ok(user.getUserId());
    }

    @Override
    public Result<User> getUser(String userId, String password) {
        Log.info("Getting user " + userId + "\n");
        if (userId == null) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
        User user;
        try {
            user = hibernate.get(User.class, userId);
        } catch (Exception e) {
            e.printStackTrace();
            Log.severe("Exception getting user " + userId + "\n");
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        if (user == null) {
            Log.info("User " + userId + " not found \n");
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }
        if ( password == null || !password.equals(user.getPassword())) {
            Log.info("Passwords don't match for user " + userId + "\n");
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }
        return Result.ok(user);
    }

    @Override
    public Result<User> updateUser(String userId, String password, User user) {
        Log.info("Updating user " + userId + "\n");
        if (userId == null || password == null)
            return Result.error(Result.ErrorCode.BAD_REQUEST);

        User oldUser;
        try {
            oldUser= hibernate.get(User.class, userId);
            if (oldUser == null){
                Log.severe("User " + userId + " could not be updated \n");
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }
            if (!password.equals(oldUser.getPassword())){
                Log.severe("User " + userId + " could not be updated \n");
                return Result.error(Result.ErrorCode.FORBIDDEN);
            }
            if (!oldUser.canUpdateUser(user)) {
                Log.severe("User " + userId + " could not be updated \n");
                return Result.error(Result.ErrorCode.BAD_REQUEST);
            }
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().equals(oldUser.getAvatarUrl())){
                if (oldUser.getAvatarUrl() != null)
                    changeReferenceOfImage(oldUser.getAvatarUrl(), false);
                changeReferenceOfImage(user.getAvatarUrl(), true);
            }
            oldUser.updateUser(user);
            hibernate.update(oldUser);
            Log.info("User " + userId + " updated \n");
        } catch (Exception e) {
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        return Result.ok(oldUser);
    }

    @Override
    public Result<User> deleteUser(String userId, String password) {
            Content contentClient = getContentClient();
            Image imageClient = getImageClient();
        if (userId == null || password == null) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        User user;
        try {
            user = hibernate.get(User.class, userId);
            if (user == null) {
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }
            if (!password.equals(user.getPassword())) {
                return Result.error(Result.ErrorCode.FORBIDDEN);
            }
            contentClient.removeAllUserVotes(userId, password, SecretKeeper.getInstance().getSecret());
            contentClient.updatePostOwner(userId, password, SecretKeeper.getInstance().getSecret());
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isBlank()) {
                imageClient.deleteImage(userId, parseUrl(user.getAvatarUrl()) , password);
            }

            hibernate.delete(user);
        } catch (Exception e) {
            e.printStackTrace();
            Log.severe("Exception deleting user " + userId + "\n");
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        return Result.ok(user);
    }

    @Override
    public Result<List<User>> searchUsers(String pattern) {
        Log.info("Searching users for pattern " + pattern + "\n");
        List<User> users;
        try {
            users = hibernate.jpql("SELECT u FROM User u WHERE u.userId LIKE '%" + pattern + "%'", User.class);
        } catch (Exception e) {
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        return Result.ok(users);
    }

    private String parseUrl(String url) {
        String[] parts = url.split("/");
        return parts[parts.length - 1];
    }

    private void changeReferenceOfImage(String imageURI, boolean addReference){
        Log.info("Changing reference of image " + imageURI + " to " + (addReference ? "add" : "remove") + "\n");
        String[] parts = imageURI.split("/");
        String imageId = parts[parts.length - 1];
        String userId = parts[parts.length - 2];
        String message = userId + "/" + imageId + " " + (addReference ? Image.ADD_IMAGE : Image.REMOVE_IMAGE);
        publisher.publish(Image.REFERENCE_COUNTER_TOPIC, message);
    }

    private void startSubscriber(KafkaSubscriber subscriber) {
        subscriber.start(new RecordProcessor() {
            @Override
            public void onReceive(ConsumerRecord<String, String> r) {
                Log.info("Received delete image request for image " + r.value() + "\n");
                String imageIdentifier = r.value();
                Hibernate.TX tx = hibernate.beginTransaction();
                List<User> users = hibernate.jpql(tx, "SELECT u FROM User u WHERE u.avatarUrl LIKE '%" + imageIdentifier + "'", User.class);
                if (!users.isEmpty()) {
                    for (User user : users) {
                        user.setAvatarUrl(null);
                    }
                    hibernate.updateAll(users);
                }
            }
        });
    }
}
