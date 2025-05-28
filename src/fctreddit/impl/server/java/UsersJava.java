package fctreddit.impl.server.java;

import fctreddit.api.data.User;
import fctreddit.api.java.Content;
import fctreddit.api.java.Image;
import fctreddit.api.java.Result;
import fctreddit.api.java.Users;
import fctreddit.impl.client.UsersClient;
import fctreddit.impl.server.Hibernate;
import fctreddit.impl.server.SecretKeeper;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

public class UsersJava extends JavaServer implements Users {

    private static Logger Log = Logger.getLogger(UsersJava.class.getName());
    private final Hibernate hibernate;

    public UsersJava() {

        hibernate = Hibernate.getInstance();
    }

    @Override
    public Result<String> createUser(User user) {
        Log.info("Creating user " + user.getUserId() + "\n");
        Hibernate hibernate = Hibernate.getInstance();
        try {
            if (user == null || !user.canBuild()) {
                return Result.error(Result.ErrorCode.BAD_REQUEST);
            }
            if (hibernate.get(User.class, user.getUserId()) != null) {
                return Result.error(Result.ErrorCode.CONFLICT);
            }
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
}
