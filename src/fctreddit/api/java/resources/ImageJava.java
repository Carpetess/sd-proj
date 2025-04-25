package fctreddit.api.java.resources;

import fctreddit.api.data.User;
import fctreddit.api.clients.clientFactories.UserClientFactory;
import fctreddit.api.java.Image;
import fctreddit.api.java.Result;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.logging.Logger;

public class ImageJava implements Image {
    private static Logger Log = Logger.getLogger(ImageJava.class.getName());

    private static final String PATH = "home/sd/images/";

    public ImageJava() {}

    @Override
    public Result<String> createImage(String userId, byte[] imageContents, String password) {
        Log.info("Creating image for user " + userId);
        if (imageContents.length == 0 || password == null)
            return Result.error(Result.ErrorCode.BAD_REQUEST);

        UserClientFactory factory;
        try {
            factory = UserClientFactory.getInstance();
        } catch (IOException e) {
            Log.severe("Exception getting client factories");
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        Result<User> user = factory.getUser(userId, password);

        if (!user.isOK()){
            Log.severe("User " + userId + " could not be authenticated");
            return Result.error(user.error());
        }

        UUID imageUUID = UUID.randomUUID();
        Path path = Paths.get(PATH, userId);
        Path filePath = Paths.get(path.toString(), imageUUID.toString());
        try {
            Files.createDirectories(path);
            Files.createFile(filePath);
            Files.write(filePath, imageContents);
        } catch (IOException e) {
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }

        Log.info("Created image " + imageUUID.toString() + " for user " + userId);
        URI image = URI.create("/image/" + userId + "/" + imageUUID.toString());
        return Result.ok(image.toString());
    }

    @Override
    public Result<byte[]> getImage(String userId, String imageId) {
        Log.info("Getting image " + imageId + " for user " + userId);

        Path imagePath = Paths.get(PATH, userId, imageId );
        File imageFile = imagePath.toFile();

        if (!imageFile.exists()){
            Log.severe("Image not found for user " + userId + " and image id " + imageId + " in path " + imagePath.toString() + " or path does not exist.");
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }

        try {
            byte[] imageContent = Files.readAllBytes(imagePath);
            return Result.ok(imageContent);
        } catch (IOException e) {
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }

    }

    @Override
    public Result<Void> deleteImage(String userId, String imageId, String password) {
        if (password == null)
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        UserClientFactory factory;
        try {
            factory = UserClientFactory.getInstance();
        } catch (IOException e) {
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        Result<User> user = factory.getUser(userId, password);
        if (!user.isOK())
            return Result.error(user.error());

        Path imagePath = Paths.get(PATH, userId, imageId);
        try {
            Files.delete(imagePath);
        } catch (NoSuchFileException e ) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        } catch (Exception e) {
            Log.severe(e.toString());
            Log.severe("Exception deleting image " + imageId + " for user " + userId + " in path " + imagePath.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }

        return Result.ok();
    }

}
