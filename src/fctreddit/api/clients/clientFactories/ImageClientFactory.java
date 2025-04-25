package fctreddit.api.clients.clientFactories;

import fctreddit.api.clients.ImageClients.ImageClient;
import fctreddit.api.clients.ImageClients.ImageGrpcClient;
import fctreddit.api.clients.ImageClients.ImageRestClient;
import fctreddit.api.java.Result;
import fctreddit.api.server.rest.ImageServer;
import fctreddit.api.server.serviceDiscovery.Discovery;

import java.io.IOException;
import java.net.URI;

public class ImageClientFactory {

    private static ImageClientFactory instance;

    Discovery discovery;

    private ImageClientFactory() throws IOException {
        discovery = new Discovery(Discovery.DISCOVERY_ADDR);
        discovery.start();
    }

    public static ImageClientFactory getInstance() throws IOException {
        if (instance == null) {
            instance = new ImageClientFactory();
        }
        return instance;
    }

    public Result<Void> deleteImage(String userId, String imageId, String password) {
        Result<ImageClient> client = getClient();
        if (!client.isOK())
            return Result.error(client.error());
        return client.value().deleteImage(userId, imageId, password);
    }


    private Result<ImageClient> getClient(){
        URI[] serviceURIs;
        try {
            serviceURIs = discovery.knownUrisOf(ImageServer.SERVICE, 1);
        } catch (InterruptedException e) {
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        URI selectedServiceURI = serviceURIs[0];

        ImageClient client = null;
        if (selectedServiceURI.toString().contains("/rest")) {
            client = new ImageRestClient(selectedServiceURI);
        } else {
            client = new ImageGrpcClient(selectedServiceURI);
        }
        return Result.ok(client);
    }


}
