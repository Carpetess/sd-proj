package fctreddit.impl.server.java;

import fctreddit.api.data.User;
import fctreddit.api.java.Image;
import fctreddit.api.java.Result;
import fctreddit.api.java.Users;
import fctreddit.impl.server.kafka.KafkaPublisher;
import fctreddit.impl.server.kafka.KafkaSubscriber;
import fctreddit.impl.server.kafka.KafkaUtils;
import fctreddit.impl.server.kafka.RecordProcessor;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class ImageJava extends JavaServer implements Image {
    private static final Logger Log = Logger.getLogger(ImageJava.class.getName());
    private static final int TIMEOUT = 30000;

    private static final String PATH = "home/sd/images/";
    private static Map<String, Map<String, Boolean>> referenceCounter = new ConcurrentHashMap<>();
    private static Map<String, Long> gracePeriod = new ConcurrentHashMap<>();

    private static KafkaPublisher kafkaPublisher;
    private static KafkaSubscriber kafkaSubscriber;

    public ImageJava() {
    }

    @Override
    public Result<String> createImage(String userId, byte[] imageContents, String password) {
        Log.info("Creating image for user " + userId);
        if (imageContents.length == 0 || password == null)
            return Result.error(Result.ErrorCode.BAD_REQUEST);

        Users usersClient = getUsersClient();
        Result<User> user = usersClient.getUser(userId, password);

        if (!user.isOK()) {
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
        gracePeriod.put(userId + "/" +  imageUUID.toString(), System.currentTimeMillis());
        return Result.ok(image.toString());
    }

    @Override
    public Result<byte[]> getImage(String userId, String imageId) {
        Log.info("Getting image " + imageId + " for user " + userId + "\n");

        String userIdImageId = userId + "/" + imageId;
        if(gracePeriod.containsKey(userIdImageId)&&System.currentTimeMillis()-gracePeriod.get(userIdImageId)>=TIMEOUT&&referenceCounter.containsKey(userIdImageId)&&referenceCounter.get(userIdImageId).isEmpty()) {
            deleteImageHelper(userId,imageId);
            referenceCounter.remove(userIdImageId);
            gracePeriod.remove(userIdImageId);
            Log.info("Deleted no get");
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }

        Path imagePath = Paths.get(PATH, userId, imageId);
        File imageFile = imagePath.toFile();


        if (!imageFile.exists()) {
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
        Users userClient = getUsersClient();
        Result<User> user = userClient.getUser(userId, password);
        if (!user.isOK())
            return Result.error(user.error());

        kafkaPublisher.publish(Image.DELETED_IMAGE_TOPIC, userId + "/" + imageId);

        return deleteImageHelper(userId, imageId);
    }

    private static Result<Void> deleteImageHelper(String userId, String imageId) {
        Path imagePath = Paths.get(PATH, userId, imageId);
        try {
            Files.delete(imagePath);
        } catch (NoSuchFileException e) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        } catch (Exception e) {
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        Log.warning("Deleted image " + imageId + " for user " + userId);
        return Result.ok();
    }

    private static void startSubscriber(KafkaSubscriber subscriber) {
        subscriber.start(new RecordProcessor() {
            @Override
            public void onReceive(ConsumerRecord<String, String> r) {
                boolean addReference = Boolean.parseBoolean(r.key());
                Log.info("Received message: " + r.value() + "with bool " + r.key() + "\n");
                String[] parts = r.value().split(" ");
                if (parts.length < 2 || parts.length > 3) {
                    Log.warning("Invalid message format: " + r.value());
                    return;
                }

                String postId = parts[0];
                String userIdImageId = parts[1];
                Log.info("postId: " + postId + " userIdImageId: " + userIdImageId+ "\n");

                if(!referenceCounter.containsKey(userIdImageId)) {
                    referenceCounter.put(userIdImageId, new ConcurrentHashMap<>());
                }

                if(addReference) {
                    Log.info("added reference for: " + userIdImageId + "\n");
                    referenceCounter.get(userIdImageId).put(postId, true);
                }else{
                    Log.info("removed reference for: " + userIdImageId+ "\n");
                    referenceCounter.get(userIdImageId).remove(postId);
                }


                Map<String, Boolean> refs = referenceCounter.get(userIdImageId);
                Long graceStart = gracePeriod.get(userIdImageId);

                if(!addReference){
                    Log.info("Maybe removing " + userIdImageId + " from reference counter: " + refs.size() + "\n");
                }
                if(graceStart == null){
                    Log.info("Not in grace");
                }else{
                    Log.info("In grace during: " + ((System.currentTimeMillis() - graceStart))+ "\n");
                }
                if ((refs == null || refs.isEmpty()) && (graceStart == null || System.currentTimeMillis() - graceStart >= TIMEOUT)) {
                    referenceCounter.remove(userIdImageId);
                    gracePeriod.remove(userIdImageId);
                    String[] split = userIdImageId.split("/");
                    if (split.length == 2) {
                        deleteImageHelper(split[0], split[1]);
                        Log.info("Deleted image for: " + userIdImageId);
                    } else {
                        Log.warning("Invalid userId/imageId format: " + userIdImageId);
                    }
                }
            }
        });
    }

    private static void gracePeriodCleanup() {
       new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(TIMEOUT);
                } catch (InterruptedException e) {
                    // Do nothing
                }
                List<String> keysToRemove = new LinkedList<>();
                synchronized (gracePeriod) {
                    for (Map.Entry<String, Long> entry : gracePeriod.entrySet()) {
                        if (System.currentTimeMillis() - entry.getValue() >= TIMEOUT)
                            keysToRemove.add(entry.getKey());
                    }
                    for (String key : keysToRemove) {
                        gracePeriod.remove(key);
                        if (!referenceCounter.containsKey(key) || referenceCounter.get(key).isEmpty()) {
                            referenceCounter.remove(key);
                            String[] parsedURI = key.split("/");
                            deleteImageHelper(parsedURI[0], parsedURI[1]);
                        }
                    }
                }
            }
        }).start();
    }

    public static void setPublisher(KafkaPublisher publisher) {
        kafkaPublisher = publisher;
    }

    public static void setSubscriber(KafkaSubscriber subscriber) {
        kafkaSubscriber = subscriber;
        startSubscriber(kafkaSubscriber);
        gracePeriodCleanup();
    }

}
