package fctreddit.impl.server.java.ImageProxy;


import com.github.scribejava.apis.ImgurApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.common.io.Files;
import com.google.gson.Gson;
import fctreddit.api.java.Image;
import fctreddit.api.java.Result;
import fctreddit.impl.server.java.JavaServer;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import static fctreddit.impl.server.APISecrets.*;


public class ImageProxy extends JavaServer implements Image {

    public static final String ALBUM_ID = "FCT Reddit";
    public static final String ALBUM_DESCRIPTION = "Images of the FCT Reddit subreddit";
    public static final String ALBUM_COVER_IMAGE_URL = "https://i.imgur.com/0000000.jpg";
    public static final String ALBUM_COVER_IMAGE_ID = "0000000";

    // album-hash
    private static final String CREATE_ALBUM_URL = "https://api.imgur.com/3/album";
    // album-hash
    private static final String DELETE_ALBUM = "https://api.imgur.com/3/album/%s";
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


    private static final int HTTP_SUCCESS = 200;
    private static final String CONTENT_TYPE_HDR = "Content-Type";
    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";

    private final Gson json;
    private final OAuth20Service service;
    private final OAuth2AccessToken accessToken;
    private Logger Log = Logger.getLogger(String.valueOf(ImageProxy.class.getName()));

    public ImageProxy() {
        super();
        json = new Gson();
        accessToken = new OAuth2AccessToken(accessTokenStr);
        service = new ServiceBuilder(apiKey).apiSecret(apiSecret).build(ImgurApi.instance());
    }

    @Override
    public Result<String> createImage(String userId, byte[] imageContents, String password) {
        UUID imageId = UUID.randomUUID();
        OAuthRequest request = new OAuthRequest(Verb.POST, UPLOAD_IMAGE);

        String imageName = String.format(userId, "/", imageId);

        request.addHeader(CONTENT_TYPE_HDR, JSON_CONTENT_TYPE);
        request.setPayload(json.toJson(new ImageUploadArguments(imageContents, imageName, userId)));

        service.signRequest(accessToken, request);

        String imageURL = String.format("https://image-proxy:8082/rest/", imageName);

        try {
            Response r = service.execute(request);

            if (r.getCode() != HTTP_SUCCESS) {
                //Operation failed
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            } else {
                Result<Void> res = addImageToAlbum(imageId.toString(), imageContents);
                if (!res.isOK())
                    return Result.error(res.error());
                //IMPLIES THAT THE IMAGE WAS UPLOADED AND ADDED TO THE ALBUM SUCCESSFULLY
                return Result.ok(imageURL.toString());
            }

        } catch (Exception e) {
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    private Result<Void> addImageToAlbum(String imageId, byte[] imageContents) {
        String requestURL = String.format(ADD_IMAGE_TO_ALBUM_URL, ALBUM_ID);

        OAuthRequest request = new OAuthRequest(Verb.POST, requestURL);

        request.addHeader(CONTENT_TYPE_HDR, JSON_CONTENT_TYPE);
        request.setPayload(json.toJson(new AddImagesToAlbumArguments(imageId)));

        service.signRequest(accessToken, request);
        try {
            Response r = service.execute(request);
            if (r.getCode() != HTTP_SUCCESS) {
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            }
            return Result.ok();

        } catch (Exception e) {
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<byte[]> getImage(String userId, String imageId) {
        String requestURL = String.format(GET_ALBUM_IMAGE, ALBUM_ID, imageId);

        OAuthRequest request = new OAuthRequest(Verb.GET, requestURL);
        service.signRequest(accessToken, request);

        try {
            Response r = service.execute(request);

            if (r.getCode() != HTTP_SUCCESS) {
                Log.severe("Operation Failed\nStatus: " + r.getCode() + "\nBody: " + r.getBody());
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            }
            Log.info("Contents of Body: " + r.getBody());
            BasicResponse body = json.fromJson(r.getBody(), BasicResponse.class);
            for (Object key : body.getData().keySet()) {
                Log.info(key + " -> " + body.getData().get(key));
            }
            return this.downloadImageBytes(body.getData().get("link").toString());
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    private Result<byte[]> downloadImageBytes(String imageURL) {
        OAuthRequest request = new OAuthRequest(Verb.GET, imageURL);

        try {
            Response r = service.execute(request);

            if (r.getCode() != HTTP_SUCCESS) {
                Log.severe("Operation to download image bytes Failed\nStatus: " + r.getCode() + "\nBody: " + r.getBody());
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            }
            byte[] imageContent = r.getStream().readAllBytes();
            Log.info("Successfully downloaded " + imageContent.length + " bytes from the image.");
            return Result.ok(imageContent);
        } catch (Exception e) {
            e.printStackTrace();
            Log.severe("Failed to download image bytes");
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<Void> deleteImage(String userId, String imageId, String password) {
        String requestURL = String.format(DELETE_IMAGE, ALBUM_ID);

        OAuthRequest request = new OAuthRequest(Verb.POST, requestURL);

        request.addHeader(CONTENT_TYPE_HDR, JSON_CONTENT_TYPE);
        request.setPayload(json.toJson(new AddImagesToAlbumArguments(imageId)));

        service.signRequest(accessToken, request);
        try {
            Response r = service.execute(request);
            if (r.getCode() != HTTP_SUCCESS) {
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            }
            return Result.ok();

        } catch (Exception e) {
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    public Result<Void> createAlbum() {
		OAuthRequest request = new OAuthRequest(Verb.POST, CREATE_ALBUM_URL);

		request.addHeader(CONTENT_TYPE_HDR, JSON_CONTENT_TYPE);
		request.setPayload(json.toJson(new CreateAlbumArguments(ALBUM_ID)));

		service.signRequest(accessToken, request);

		try {
			Response r = service.execute(request);

			if(r.getCode() != HTTP_SUCCESS) {
				//Operation failed
				Log.severe("Operation Failed\nStatus: " + r.getCode() + "\nBody: " + r.getBody());
				return Result.error(Result.ErrorCode.INTERNAL_ERROR);
			} else {
				BasicResponse body = json.fromJson(r.getBody(), BasicResponse.class);
				Log.info("Contents of Body: " + r.getBody());
				Log.info("Operation Succedded\nAlbum name: " + ALBUM_ID + "\nAlbum ID: " + body.getData().get("id"));
                return Result.ok();
			}
		} catch (Exception e) {
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    public Result<Void> deleteAlbum() {
        OAuthRequest request = new OAuthRequest(Verb.POST, DELETE_ALBUM);
        return Result.ok();
    }
}
