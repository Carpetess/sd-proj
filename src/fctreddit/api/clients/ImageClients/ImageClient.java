package fctreddit.api.clients.ImageClients;

import fctreddit.api.java.Result;

public abstract class ImageClient {

    /**
     * Deletes the image with that imageId that belongs to the user with that userId.
     * @param userId user whose image will be deleted.
     * @param password password of that user.
     * @param imageId imageId of the image that is to be deleted.
     * @return NO_CONTENT if the image was deleted successfully, FORBIDDEN if the password of that user did not match and
     * NOT_FOUND if the user or the image didn't exist.
     */
    abstract public Result<Void> deleteImage(String userId, String password, String imageId);
}
