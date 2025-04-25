package fctreddit.api.clients.clientFactories;

import fctreddit.api.data.User;
import fctreddit.api.clients.UserClients.UsersClient;
import fctreddit.api.clients.UserClients.UsersGrpcClient;
import fctreddit.api.clients.UserClients.UsersRestClient;
import fctreddit.api.java.Result;
import fctreddit.api.server.rest.UsersServer;
import fctreddit.api.server.serviceDiscovery.Discovery;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

public class UserClientFactory {

    private static UserClientFactory instance;
    private static final Logger Log = Logger.getLogger(UserClientFactory.class.getName());

    Discovery discovery ;

    private UserClientFactory() throws IOException {
        discovery = new Discovery(Discovery.DISCOVERY_ADDR);
        discovery.start();
    }

    public static UserClientFactory getInstance() throws IOException {
        if (instance == null)
            instance = new UserClientFactory();
        return instance;
    }

    public Result<User> getUser(String userId, String password) {
        Result<UsersClient> client = getClient();
        if (!client.isOK())
            return Result.error(client.error());
        return client.value().getUser(userId, password);
    }

    private Result<UsersClient> getClient(){
        URI[] serviceURIs;
        try{
            serviceURIs = discovery.knownUrisOf(UsersServer.SERVICE, 1);
        } catch (InterruptedException e) {
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        URI selectedServiceURI = serviceURIs[0];

        UsersClient client = null;
        if (selectedServiceURI.toString().contains("/rest")) {
            client = new UsersRestClient(selectedServiceURI);
        } else {
            client = new UsersGrpcClient(selectedServiceURI);
        }
        return Result.ok(client);
    }

}
