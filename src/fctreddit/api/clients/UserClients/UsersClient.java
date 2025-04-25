package fctreddit.api.clients.UserClients;

import fctreddit.api.data.User;
import fctreddit.api.java.Result;
import fctreddit.api.java.Users;
import java.util.List;

public abstract class UsersClient implements Users {

    /**
     * Gets the user if it exists and if the password matches.
     * @param userId target user of this operation.
     * @param password password of the user.
     * @return <OK, User> if the user exists and the password matches, NOT_FOUND if the user doesn't exist
     * FORBIDDEN if the password doesn't match.
     */
    abstract public Result<User> getUser(String userId, String password);

    /**
     * Updates the user if it exists.
     * @param user target user
     * @return <NO_CONTENT> if the user was updated successfully, NOT_FOUND if it doesn't exist
     * , FORBIDDEN if the password doesn't match.
     */

    abstract public Result<User> updateUser(String userId, String password, User user);

    abstract public Result<User> deleteUser(String userId, String password);
    
    abstract public Result<String> createUser(User user);
    
    abstract public Result<List<User>> searchUsers(String pattern);



}
