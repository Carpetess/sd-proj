package fctreddit.impl.server.java;

import fctreddit.api.data.Post;
import fctreddit.api.data.User;
import fctreddit.api.java.Content;
import fctreddit.api.java.Image;
import fctreddit.api.java.Result;
import fctreddit.api.java.Users;
import fctreddit.impl.client.UsersClient;
import fctreddit.impl.server.Hibernate;
import fctreddit.impl.server.SecretKeeper;
import fctreddit.impl.server.kafka.KafkaPublisher;
import fctreddit.impl.server.kafka.KafkaSubscriber;
import fctreddit.impl.server.kafka.RecordProcessor;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

public class UsersJava extends JavaServer implements Users {

    private static Logger Log = Logger.getLogger(UsersJava.class.getName());
    private final Hibernate hibernate;
    private static KafkaPublisher publisher;

    public UsersJava() {

        hibernate = Hibernate.getInstance();
    }

    @Override
    public Result<String> createUser(User user) {
        Hibernate.TX tx = hibernate.beginTransaction();
        try {
            if (user == null || !user.canBuild()) {
                hibernate.abortTransaction(tx);
                return Result.error(Result.ErrorCode.BAD_REQUEST);
            }
            if (hibernate.get(tx, User.class, user.getUserId()) != null) {
                hibernate.abortTransaction(tx);
                return Result.error(Result.ErrorCode.CONFLICT);
            }
            hibernate.persist(tx, user);
            hibernate.commitTransaction(tx);
            if(user.getAvatarUrl()!=null) {
                changeReferenceOfImage(user,true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.severe("Exception persisting user " + user.getUserId());
            hibernate.abortTransaction(tx);
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        return Result.ok(user.getUserId());
    }

    @Override
    public Result<User> getUser(String userId, String password) {
        Log.info("Getting user " + userId);
        if (userId == null) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
        User user;
        try {
            user = hibernate.get(User.class, userId);
        } catch (Exception e) {
            e.printStackTrace();
            Log.severe("Exception getting user " + userId);
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        if (user == null) {
            Log.info("User " + userId + " not found");
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }
        if (password == null || !password.equals(user.getPassword())) {
            Log.info("Passwords don't match for user " + userId);
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }
        return Result.ok(user);
    }

    @Override
    public Result<User> updateUser(String userId, String password, User user) {
        if (userId == null || password == null)
            return Result.error(Result.ErrorCode.BAD_REQUEST);

        Hibernate.TX tx = hibernate.beginTransaction();
        User oldUser;
        try {
            oldUser = hibernate.get(tx, User.class, userId);
            if (oldUser == null){
                hibernate.abortTransaction(tx);
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }
            if (!password.equals(oldUser.getPassword())){
                hibernate.abortTransaction(tx);
                return Result.error(Result.ErrorCode.FORBIDDEN);
            }
            if (!oldUser.canUpdateUser(user)) {
                hibernate.abortTransaction(tx);
                Log.severe("User " + userId + " could not be updated");
                return Result.error(Result.ErrorCode.BAD_REQUEST);
            }

            if(user.getAvatarUrl()!=null){
                if(oldUser.getAvatarUrl()!=null&&!oldUser.getAvatarUrl().equals(user.getAvatarUrl())){
                    changeReferenceOfImage(oldUser,false);
                    if(!oldUser.getAvatarUrl().equals(user.getAvatarUrl()))
                        changeReferenceOfImage(user,true);
                }

            }
            oldUser.updateUser(user);
            hibernate.update(tx, oldUser);
            hibernate.commitTransaction(tx);
        } catch (Exception e) {
            Log.severe(e.toString());
            hibernate.abortTransaction(tx);
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        return Result.ok(oldUser);
    }


    public static void setPublisher(KafkaPublisher publisher) {
        UsersJava.publisher = publisher;
    }

    public static void setSubscriber(KafkaSubscriber subscriber) {
        startSubscriber(subscriber);
    }


    private static void startSubscriber(KafkaSubscriber subscriber) {
        subscriber.start(new RecordProcessor() {
            Hibernate hibernate = Hibernate.getInstance();

            @Override
            public void onReceive(ConsumerRecord<String, String> r) {
                Hibernate.TX tx = hibernate.beginTransaction();
                String imageToRemove = r.value();
                hibernate.sql(tx, "UPDATE User u SET u.avatarUrl=NULL WHERE u.avatarUrl LIKE '%" + imageToRemove + "'");
                hibernate.commitTransaction(tx);
            }
        });
    }

    @Override
    public Result<User> deleteUser(String userId, String password) {

        Content contentClient = getContentClient();
        Image imageClient = getImageClient();
        if (userId == null || password == null) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        Hibernate.TX tx = hibernate.beginTransaction();
        User user;
        try {
            user = hibernate.get(tx, User.class, userId);
            if (user == null) {
                hibernate.abortTransaction(tx);
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }
            if (!password.equals(user.getPassword())) {
                hibernate.abortTransaction(tx);
                return Result.error(Result.ErrorCode.FORBIDDEN);
            }
            contentClient.removeUserTrace(userId, SecretKeeper.getInstance().getSecret());
            changeReferenceOfImage(user,false);
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isBlank()) {
                imageClient.deleteImage(userId, parseUrl(user.getAvatarUrl()), password);
            }

            hibernate.delete(tx, user);
            hibernate.commitTransaction(tx);
        } catch (Exception e) {
            e.printStackTrace();
            Log.severe("Exception deleting user " + userId);
            hibernate.abortTransaction(tx);
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        return Result.ok(user);
    }

    @Override
    public Result<List<User>> searchUsers(String pattern) {
        List<User> users = new LinkedList<>();
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

    private void changeReferenceOfImage(User user, boolean add) {
        String[] slice = user.getAvatarUrl().split("/");
        String imageId = slice[slice.length - 1];
        String userId = slice[slice.length - 2];
        String message = user.getUserId() + " " + userId + "/" + imageId;
        if (add) {
            publisher.publish(Image.IMAGE_REFERENCE_COUNTER_TOPIC, "true", message);
        } else {
            publisher.publish(Image.IMAGE_REFERENCE_COUNTER_TOPIC, "false", message);
        }
    }
}
