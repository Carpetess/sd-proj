package fctreddit.impl.server.java;

import fctreddit.api.data.User;
import fctreddit.api.java.Image;
import fctreddit.api.java.Result;
import fctreddit.api.java.Users;
import fctreddit.impl.server.repl.KafkaPublisher;
import fctreddit.impl.server.repl.KafkaSubscriber;
import fctreddit.impl.server.repl.KafkaUtils;
import fctreddit.impl.server.repl.RecordProcessor;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class ImageJava extends JavaServer implements Image {
    private static Logger Log = Logger.getLogger(ImageJava.class.getName());

    private static final String PATH = "home/sd/images/";

    private static final Object lock = new Object();
    private static KafkaSubscriber subscriber = null;
    private static KafkaPublisher publisher = null;
    private static final Map<String, Long> imageReferenceCounter = new ConcurrentHashMap<>();
    private static final Map<String, Void> gracePeriodImages = new ConcurrentHashMap<>();

    public ImageJava() {
        if (subscriber == null || publisher == null){
            synchronized (lock){
                if (subscriber == null) {
                    Log.info("Starting subscriber");
                    KafkaUtils.createTopic(REFERENCE_COUNTER_TOPIC);
                    subscriber = KafkaSubscriber.createSubscriber("kafka:9092", List.of(REFERENCE_COUNTER_TOPIC));
                    startSubscriber(subscriber);
                }
                if (publisher == null) {
                    Log.info("Starting publisher");
                    KafkaUtils.createTopic(DELETED_IMAGE_TOPIC);
                    publisher = KafkaPublisher.createPublisher("kafka:9092");
                }
            }
        }

    }

    @Override
    public Result<String> createImage(String userId, byte[] imageContents, String password) {
        Log.info("Creating image for user " + userId + "\n");
        if (imageContents.length == 0 || password == null)
            return Result.error(Result.ErrorCode.BAD_REQUEST);

        Users usersClient = getUsersClient();
        Result<User> user = usersClient.getUser(userId, password);

        if (!user.isOK()) {
            Log.severe("User " + userId + " could not be authenticated \n");
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
            Log.severe(e.toString() + "\n");
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }

        Log.info("Created image " + imageUUID + " for user " + userId + "\n");
        URI image = URI.create("/image/" + userId + "/" + imageUUID);

        imageReferenceCounter.put(userId + "/" + imageUUID, 0L);
        startImageCounter(userId, imageUUID.toString());
        return Result.ok(image.toString());
    }

    @Override
    public Result<byte[]> getImage(String userId, String imageId) {
        Log.info("Getting image " + imageId + " for user " + userId + "\n");

        Path imagePath = Paths.get(PATH, userId, imageId);
        File imageFile = imagePath.toFile();

        if (!imageFile.exists()) {
            Log.severe("Image not found for user " + userId + " and image id " + imageId + " in path " + imagePath.toString() + " or path does not exist. \n");
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }

        try {
            byte[] imageContent = Files.readAllBytes(imagePath);
            return Result.ok(imageContent);
        } catch (IOException e) {
            Log.severe(e.toString() + "\n");
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }

    }

    @Override
    public Result<Void> deleteImage(String userId, String imageId, String password) {
        Log.info("Deleting image " + imageId + " for user " + userId + "\n");
        if (password == null)
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        Users userClient = getUsersClient();
        Result<User> user = userClient.getUser(userId, password);
        if (!user.isOK())
            return Result.error(user.error());
        publisher.publish(DELETED_IMAGE_TOPIC, userId + "/" + imageId);
        imageReferenceCounter.remove(userId + "/" + imageId);

        return deleteImageHelper(userId, imageId);
    }

    private Result<Void> deleteImageHelper(String userId, String imageId) {
        Path imagePath = Paths.get(PATH, userId, imageId);
        try {
            Files.delete(imagePath);
        } catch (NoSuchFileException e) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        } catch (Exception e) {
            Log.severe(e.toString() + "\n");
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }

        return Result.ok();
    }

    private void startSubscriber(KafkaSubscriber subscriber) {
        subscriber.start(new RecordProcessor() {
            @Override
            public void onReceive(ConsumerRecord<String, String> r) {
                Log.info("Received delete image request for image " + r.value() + "\n");
                String[] parts = r.value().split(" ");
                String id = parts[0];
                long referenceChange = ADD_IMAGE.equals(parts[1]) ? 1 : -1;
                String[] idParts = id.split("/");
                if (!gracePeriodImages.containsKey(id)){
                synchronized (imageReferenceCounter) {
                    Long referenceCount = imageReferenceCounter.get(id);
                    if (referenceCount != null) {
                        if (referenceCount + referenceChange == 0){
                            imageReferenceCounter.remove(id);
                            deleteImageHelper(idParts[0], idParts[1]);
                        } else {
                            imageReferenceCounter.put(id, referenceCount + referenceChange);
                        }
                    }
                }
                }
            }
        });
    }

    private void startImageCounter(String userId, String imageId) {
        new Thread(() -> {
            gracePeriodImages.put(userId + "/" + imageId, null);
            try {
                Thread.sleep(30000);
            } catch (InterruptedException e) {
                // Do nothing
            }
            gracePeriodImages.remove(userId + "/" + imageId);
                Long referenceCount = imageReferenceCounter.get(userId + "/" + imageId);
                if (referenceCount != null && referenceCount == 0) {
                    imageReferenceCounter.remove(userId + "/" + imageId);
                    deleteImageHelper(userId, imageId);
                }
        }
        ).start();
    }
}
