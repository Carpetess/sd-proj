package fctreddit.impl.server.rest;

import fctreddit.impl.server.java.ImageJava;
import fctreddit.api.java.Result;
import fctreddit.api.rest.RestImage;
import jakarta.ws.rs.WebApplicationException;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

import static fctreddit.impl.server.ErrorParser.errorCodeToStatus;

public class ImageResource implements RestImage {
    private Logger Log = Logger.getLogger(String.valueOf(ImageResource.class));
    private ImageJava impl;

    public ImageResource() {
        impl = new ImageJava();
    }

    @Override
    public String createImage(String userId, byte[] imageContents, String password) {
        Log.info("createImage by user: " + userId);

        Result<String> res = impl.createImage(userId, imageContents, password);

        if(!res.isOK()){
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
        URI finalURI = URI.create(ImageServer.serverURI.toString() + res.value());
        return finalURI.toString();
    }

    @Override
    public byte[] getImage(String userId, String imageId) {
        Log.info("getImage of user: " + userId);

        Result<byte[]> res = impl.getImage(userId, imageId);

        if(!res.isOK()){
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
        return res.value();
    }

    @Override
    public void deleteImage(String userId, String imageId, String password) throws IOException {
        Log.info("deleteImage by user: " + userId);

        Result<Void> res = impl.deleteImage(userId, imageId, password);

        if(!res.isOK()){
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
    }
}
