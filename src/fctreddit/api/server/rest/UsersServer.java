package fctreddit.api.server.rest;

import fctreddit.api.clients.clientFactories.ContentClientFactory;
import fctreddit.api.clients.clientFactories.ImageClientFactory;
import fctreddit.api.rest.UserResource;
import fctreddit.api.server.serviceDiscovery.Discovery;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;

public class UsersServer {

    private static Logger Log = Logger.getLogger(UsersServer.class.getName());

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    private static Discovery discovery;

    public static int PORT = 8080;
    public static final String SERVICE = "Users";
    private static final String SERVER_URI_FMT = "http://%s:%s/rest";


    public static void main(String[] args) {
        try {
            ResourceConfig config = new ResourceConfig();
            config.register(UserResource.class);

            String ip = InetAddress.getLocalHost().getHostAddress();
            String serverURI = String.format(SERVER_URI_FMT, ip, PORT);
            discovery = new Discovery(Discovery.DISCOVERY_ADDR, SERVICE, serverURI);
            discovery.start();
            ContentClientFactory.getInstance().setDiscovery(discovery);
            ImageClientFactory.getInstance().setDiscovery(discovery);
            JdkHttpServerFactory.createHttpServer( URI.create(serverURI), config);

            Log.info(String.format("%s Server ready @ %s\n",  SERVICE, serverURI));

        } catch( Exception e) {
            Log.severe(e.getMessage());
        }
    }
}
