package fctreddit.impl.server.java.ImageProxy;


import com.github.scribejava.apis.ImgurApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.gson.Gson;
import fctreddit.api.data.User;
import fctreddit.api.java.Image;
import fctreddit.api.java.Result;
import fctreddit.impl.server.java.JavaServer;
import fctreddit.impl.server.rest.ImageProxyServer;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static fctreddit.impl.server.APISecrets.*;


public class ImageProxyJava extends JavaServer implements Image {

    private static final String USERNAME = "Carpetesss";

    // album-hash
    private static final String CREATE_ALBUM_URL = "https://api.imgur.com/3/album";
    // album-hash
    private static final String ALBUM_IMAGES = "https://api.imgur.com/3/album/%s/images";
    // album-hash -> image-hash
    private static final String GET_ALBUM_IMAGE = "https://api.imgur.com/3/album/%s/image/%s";
    // album-hash
    private static final String ADD_IMAGE_TO_ALBUM_URL = "https://api.imgur.com/3/album/%s/add";
    // receives image-ids as param
    private static final String UPLOAD_IMAGE = "https://api.imgur.com/3/image";
    // image-hash
    private static final String DELETE_IMAGE = "https://api.imgur.com/3/image/%s";
    // album-hash
    private static final String GET_ALBUM = "https://api.imgur.com/3/account/" + USERNAME + "/album/%s";

    private static final String GET_ALBUMS = "https://api.imgur.com/3/account/" + USERNAME + "/albums/";

    private static final int HTTP_SUCCESS = 200;
    private static final String CONTENT_TYPE_HDR = "Content-Type";
    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";

    private final Gson json;
    private final OAuth20Service service;
    private final OAuth2AccessToken accessToken;
    private Logger Log = Logger.getLogger(String.valueOf(ImageProxyJava.class.getName()));
    private String associatedAlbumId;

    public ImageProxyJava() {
        super();
        associatedAlbumId = ImageProxyServer.getAssociatedAlbumId();
        json = new Gson();
        accessToken = new OAuth2AccessToken(accessTokenStr);
        service = new ServiceBuilder(apiKey).apiSecret(apiSecret).build(ImgurApi.instance());
    }

    @Override
    public Result<String> createImage(String userId, byte[] imageContents, String password) {
        Log.info("Creating image for user " + userId + "\n");
        OAuthRequest request = new OAuthRequest(Verb.POST, UPLOAD_IMAGE);
        String imageName = UUID.randomUUID().toString();

        request.addHeader(CONTENT_TYPE_HDR, JSON_CONTENT_TYPE);
        request.setPayload(json.toJson(new ImageUploadArguments(imageContents, imageName, userId)));

        service.signRequest(accessToken, request);
        try {
            Response r = service.execute(request);
            if (r.getCode() != HTTP_SUCCESS) {
                Log.severe("Operation Failed\nStatus: " + r.getCode() + "\nBody: " + r.getBody());
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            } else {
                BasicResponse body = json.fromJson(r.getBody(), BasicResponse.class);
                String imageId = body.getData().get("id").toString();
                Log.info("Image was uploaded\n");
                Result<Void> res = addImageToAlbum(imageId);
                if (!res.isOK())
                    return Result.error(res.error());
                Log.info("Image was added to album " + associatedAlbumId + "\n");
                return Result.ok(String.format("/image/%s/%s", userId, imageId));
            }
        } catch (Exception e) {
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    private Result<Void> addImageToAlbum(String imageId) {
        Log.info("Adding image "+ imageId +" to album " + associatedAlbumId + "\n");
        String requestURL = String.format(ADD_IMAGE_TO_ALBUM_URL, associatedAlbumId);
        OAuthRequest request = new OAuthRequest(Verb.POST, requestURL);

        request.addHeader(CONTENT_TYPE_HDR, JSON_CONTENT_TYPE);
        request.setPayload(json.toJson(new AddImagesToAlbumArguments(imageId)));

        service.signRequest(accessToken, request);
        try {
            Response r = service.execute(request);
            if (r.getCode() != HTTP_SUCCESS) {
                Log.severe("Operation Failed\nStatus: " + r.getCode() + "\nBody: " + r.getBody());
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            }
            return Result.ok();
        } catch (Exception e) {
            Log.severe(e.toString() + "\n");
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<byte[]> getImage(String userId, String imageId) {
        Log.info("Getting image " + imageId + "\n");
        Result<BasicResponse> res = getImageObject(userId, imageId);
        if (!res.isOK()) {
            Log.severe("Did not get image properly \n");
            return Result.error(res.error());
        }
        return this.downloadImageBytes(res.value().getData().get("link").toString());
    }

    private Result<byte[]> downloadImageBytes(String imageURL) {
        OAuthRequest request = new OAuthRequest(Verb.GET, imageURL);
        try {
            Response r = service.execute(request);

            if (r.getCode() != HTTP_SUCCESS) {
                Log.severe("Operation to download image bytes Failed\nStatus: " + r.getCode() + "\nBody: " + r.getBody() + "\n");
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            }
            byte[] imageContent = r.getStream().readAllBytes();
            Log.info("Successfully downloaded " + imageContent.length + " bytes from the image.\n");
            return Result.ok(imageContent);
        } catch (Exception e) {
            e.printStackTrace();
            Log.severe("Failed to download image bytes\n");
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    private Result<BasicResponse> getImageObject(String userId, String imageId) {
        Log.info("Getting image object " + imageId + "\n");
        String requestURL = String.format(GET_ALBUM_IMAGE, associatedAlbumId, imageId);
        OAuthRequest request = new OAuthRequest(Verb.GET, requestURL);
        service.signRequest(accessToken, request);
        try {
            Response r = service.execute(request);
            if (r.getCode() != HTTP_SUCCESS) {
                if (r.getCode() == 404) {
                    Log.severe("Image not found" + imageId + "\n");
                    return Result.error(Result.ErrorCode.NOT_FOUND);
                }
                Log.severe("Operation to download image bytes Failed\nStatus: " + r.getCode() + "\nBody: " + r.getBody() + "\n");
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            }
            Log.info("Contents of Body: " + r.getBody());
            BasicResponse body = json.fromJson(r.getBody(), BasicResponse.class);
            if (!body.getData().get("description").toString().equals(userId)) {
                Log.severe("Image is not from specified user");
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }
            return Result.ok(body);
        } catch (Exception e) {
            Log.severe(e.toString()+ "\n");
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<Void> deleteImage(String userId, String imageId, String password) {
        Result<User> userRes = getUsersClient().getUser(userId, password);
        if (!userRes.isOK()) {
            Log.severe("Result is not ok");
            return Result.error(userRes.error());
        }
        Result<BasicResponse> imageRes = getImageObject(userId, imageId);
        if (!imageRes.isOK()) {
            Log.severe("Result is not ok: " + imageRes.error()+ "\n");
            return Result.error(imageRes.error());
        }

        String requestURL = String.format(DELETE_IMAGE, imageId);
        OAuthRequest request = new OAuthRequest(Verb.DELETE, requestURL);

        request.addHeader(CONTENT_TYPE_HDR, JSON_CONTENT_TYPE);
        service.signRequest(accessToken, request);
        Log.warning("AAA");
        try {
            Response r = service.execute(request);
            if (r.getCode() != HTTP_SUCCESS) {
                Log.severe("Operation Failed\nStatus: " + r.getCode() + "\nBody: " + r.getBody());
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            }
            return Result.ok();
        } catch (Exception e) {
            Log.severe(e.toString()+ "\n");
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }


    public Result<String> createAlbum(String hostname) {
        Log.info("Creating album for host " + hostname + "\n");
        Result<String> albumName = findAlbumName(hostname);
        if (albumName.isOK()){
            Log.info("Album already exists for host " + hostname);
            return albumName;
        }
        Log.info("Album does not exist for host " + hostname);
        OAuthRequest request = new OAuthRequest(Verb.POST, CREATE_ALBUM_URL);
        request.addHeader(CONTENT_TYPE_HDR, JSON_CONTENT_TYPE);
        request.setPayload(json.toJson(new CreateAlbumArguments(hostname)));
        service.signRequest(accessToken, request);

        try {
            Response r = service.execute(request);
            if (r.getCode() != HTTP_SUCCESS) {
                Log.severe("Operation Failed\nStatus: " + r.getCode() + "\nBody: " + r.getBody());
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            } else {
                BasicResponse body = json.fromJson(r.getBody(), BasicResponse.class);
                String albumId = body.getData().get("id").toString();
                return Result.ok(albumId);
            }
        } catch (Exception e) {
            Log.severe(e.toString()+ "\n");
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    private Result<String> findAlbumName(String hostname) {
        Log.info("Check if album already exist\n");
        Result<List<String>> albums = getAllAlbums();
        if (!albums.isOK()){
            Log.info("Error getting albums\n");
            return Result.error(albums.error());
        }
        for (String album : albums.value()) {
            if (albumMatches(album, hostname).isOK()){
                Log.info("Found album " + album + "\n");
                return Result.ok(album);
            }
        }
        return Result.error(Result.ErrorCode.NOT_FOUND);
    }

    private Result<List<String>> getAllAlbums() {
        Log.info("Getting all albums\n");
        OAuthRequest getAllAlbums = new OAuthRequest(Verb.GET, GET_ALBUMS);
        getAllAlbums.addHeader(CONTENT_TYPE_HDR, JSON_CONTENT_TYPE);

        service.signRequest(accessToken, getAllAlbums);

        try {
            Response r = service.execute(getAllAlbums);
            if (r.getCode() != HTTP_SUCCESS) {
                Log.severe("Operation Failed\nStatus: " + r.getCode() + "\nBody: " + r.getBody());
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            } else {
                BasicResponseArray body = json.fromJson(r.getBody(), BasicResponseArray.class);
                if(body.getData() != null){
                    List<String> result = body.getData().stream().map(value -> ((Map<?, ?>) value).get("id").toString()).toList();
                    return Result.ok(result);
                }
                return Result.ok(new LinkedList<>());
            }
        } catch (Exception e) {
            Log.severe(e.toString()+ "\n");
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    private Result<Void> albumMatches(String albumId, String albumName) {
        OAuthRequest getAlbum = new OAuthRequest(Verb.GET, String.format(GET_ALBUM, albumId));
        getAlbum.addHeader(CONTENT_TYPE_HDR, JSON_CONTENT_TYPE);

        service.signRequest(accessToken, getAlbum);

        try {
            Response r = service.execute(getAlbum);
            if (r.getCode() != HTTP_SUCCESS) {
                Log.severe("Operation Failed\nStatus: " + r.getCode() + "\nBody: " + r.getBody());
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            } else {
                BasicResponse body = json.fromJson(r.getBody(), BasicResponse.class);
                if (body.getData().get("title").equals(albumName))
                    return Result.ok();
                else
                    return Result.error(Result.ErrorCode.NOT_FOUND);
            }

        } catch (Exception e) {
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }

    }

    public void deleteAlbum(String albumId) {
        Log.info("Deleting album " + albumId + "\n");
        Result<List<String>> images = getAllAlbumImages(albumId);
        if (!images.isOK()) {
            images.error();
            return;
        }
        List<OAuthRequest> deleteImages = new LinkedList<>();

        for (String imageId : images.value()) {
            OAuthRequest deleteImage = new OAuthRequest(Verb.DELETE, DELETE_IMAGE.replace("%s", imageId));
            deleteImage.addHeader(CONTENT_TYPE_HDR, JSON_CONTENT_TYPE);
            deleteImages.add(deleteImage);
            service.signRequest(accessToken, deleteImage);
        }
        for (OAuthRequest deleteImage : deleteImages) {
            Log.info("Deleting image " + deleteImage.getUrl() + "\n");
            new Thread ( () -> {
                try {
                    service.execute(deleteImage);
                } catch (Exception e) {
                   Log.severe(e.toString()+ "\n");
                }
            }).start();
        }
    }

    private Result<List<String>> getAllAlbumImages(String albumId) {
        Log.info("Getting all album images \n");
        OAuthRequest getAlbumImages = new OAuthRequest(Verb.GET, ALBUM_IMAGES.replace("%s", albumId));
        getAlbumImages.addHeader(CONTENT_TYPE_HDR, JSON_CONTENT_TYPE);
        service.signRequest(accessToken, getAlbumImages);

        try {
            Response r = service.execute(getAlbumImages);
            if (r.getCode() != HTTP_SUCCESS) {
                Log.severe("Operation Failed\nStatus: " + r.getCode() + "\nBody: " + r.getBody());
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            } else {
                BasicResponseArray body = json.fromJson(r.getBody(), BasicResponseArray.class);
                if (body.getData() != null){
                    List<String> listOfIds = body.getData().stream()
                            .map(value -> ((Map<?, ?>) value).get("id").toString()).toList();
                    return Result.ok(listOfIds);
                }
                return Result.ok(new LinkedList<>());

            }
        } catch (Exception e) {
            Log.severe(e.toString()+ "\n");
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }
}