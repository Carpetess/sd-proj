package fctreddit.api.clients.clientFactories;

import fctreddit.api.clients.ContentClients.ContentClient;
import fctreddit.api.clients.ContentClients.ContentGrpcClient;
import fctreddit.api.clients.ContentClients.ContentRestClient;
import fctreddit.api.data.Post;
import fctreddit.api.java.Result;
import fctreddit.api.server.rest.ContentServer;
import fctreddit.api.server.serviceDiscovery.Discovery;

import java.io.IOException;
import java.net.URI;

public class ContentClientFactory {

    private static ContentClientFactory instance;

    Discovery discovery;

    private ContentClientFactory() throws IOException {
        discovery = new Discovery(Discovery.DISCOVERY_ADDR);
        discovery.start();
    }

    public static ContentClientFactory getInstance() throws IOException {
        if (instance == null) {
            instance = new ContentClientFactory();
        }
        return instance;
    }

    public Result<Void> removeAllUserVotes(String userId, String password) {
        Result<ContentClient> client = getClient();
        if (!client.isOK())
            return Result.error(client.error());
        return client.value().removeAllUserVotes(userId, password);
    }

    public Result<Void> updatePostOwner(String authorId, String password) {
        Result<ContentClient> client = getClient();
        if (!client.isOK())
            return Result.error(client.error());
        return client.value().updatePostOwner(authorId, password);
    }


    private Result<ContentClient> getClient(){
        URI[] serviceURIs;
        try {
            serviceURIs = discovery.knownUrisOf(ContentServer.SERVICE, 1);
        } catch (InterruptedException e) {
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        URI selectedServiceURI = serviceURIs[0];

        ContentClient client = null;
        if (selectedServiceURI.toString().contains("/rest")) {
            client = new ContentRestClient(selectedServiceURI);
        } else {
            client = new ContentGrpcClient(selectedServiceURI);
        }
        return Result.ok(client);
    }
}
