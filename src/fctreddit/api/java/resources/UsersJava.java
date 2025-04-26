package fctreddit.api.java.resources;

import fctreddit.api.clients.ImageClients.ImageClient;
import fctreddit.api.clients.clientFactories.ContentClientFactory;
import fctreddit.api.clients.clientFactories.ImageClientFactory;
import fctreddit.api.data.User;
import fctreddit.api.java.Result;
import fctreddit.api.java.Users;
import fctreddit.api.server.persistence.Hibernate;

import java.io.IOException;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

public class UsersJava implements Users {

    private static Logger Log = Logger.getLogger(UsersJava.class.getName());
    private final Hibernate hibernate;

    public UsersJava() {
        hibernate = Hibernate.getInstance();
    }

    @Override
    public Result<String> createUser(User user) {
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
            e.printStackTrace();
            Log.severe("Exception persisting user " + user.getUserId());
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
        if ( password == null || !password.equals(user.getPassword())) {
            Log.info("Passwords don't match for user " + userId);
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }
        return Result.ok(user);
    }

    @Override
    public Result<User> updateUser(String userId, String password, User user) {
        if (userId == null || password == null)
            return Result.error(Result.ErrorCode.BAD_REQUEST);

        User oldUser;
        try {
            oldUser= hibernate.get(User.class, userId);
            if (oldUser == null)
                return Result.error(Result.ErrorCode.NOT_FOUND);


            if (!password.equals(oldUser.getPassword()))
                return Result.error(Result.ErrorCode.FORBIDDEN);

            if (!oldUser.canUpdateUser(user)) {
                Log.severe("User " + userId + " could not be updated");
                return Result.error(Result.ErrorCode.BAD_REQUEST);
            }
            oldUser.updateUser(user);
            hibernate.update(oldUser);
        } catch (Exception e) {
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        return Result.ok(oldUser);
    }

    @Override
    public Result<User> deleteUser(String userId, String password) {
        ContentClientFactory contentClientFactory;
        ImageClientFactory imageClientFactory;
        contentClientFactory = ContentClientFactory.getInstance();
        imageClientFactory = ImageClientFactory.getInstance();
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
            contentClientFactory.removeAllUserVotes(userId, password);
            contentClientFactory.updatePostOwner(userId, password);
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isBlank()) {
                imageClientFactory.deleteImage(userId, parseUrl(user.getAvatarUrl()) , password);
            }

            hibernate.delete(user);
        } catch (Exception e) {
            e.printStackTrace();
            Log.severe("Exception deleting user " + userId);
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
}
