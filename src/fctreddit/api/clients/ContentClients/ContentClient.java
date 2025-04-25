package fctreddit.api.clients.ContentClients;

import fctreddit.api.java.Result;


public abstract class ContentClient {
    /**
     * When a user is deleted this method calls the content server and removes all interactions a user has ever made
     * @param userId target user of this operation
     * @return NO_CONTENT if all the interactions were removed, INTERNAL_SERVER_ERROR otherwise.
     */
    abstract public Result<Void> removeAllUserVotes(String userId, String password);

    abstract public Result<Void> updatePostOwner(String authorId, String password);


}
