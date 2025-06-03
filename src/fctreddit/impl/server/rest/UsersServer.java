package fctreddit.impl.server.rest;

import fctreddit.impl.server.Discovery;
import fctreddit.impl.server.SecretKeeper;
import fctreddit.impl.server.java.JavaServer;
import fctreddit.impl.server.java.UsersJava;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import javax.net.ssl.SSLContext;
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
    private static final String SERVER_URI_FMT = "https://%s:%s/rest";

    public static void main(String[] args) {
        new UsersJava();
        try {
            ResourceConfig config = new ResourceConfig();
            config.register(UserResource.class);

            SecretKeeper.getInstance().setSecret(args[args.length-1]);
            String hostName = InetAddress.getLocalHost().getHostName();
            String serverURI = String.format(SERVER_URI_FMT, hostName, PORT);
            discovery = new Discovery(Discovery.DISCOVERY_ADDR, SERVICE, serverURI);
            discovery.start();
            JavaServer.setDiscovery(discovery);
            JdkHttpServerFactory.createHttpServer( URI.create(serverURI), config, SSLContext.getDefault());

            Log.info(String.format("%s Server ready @ %s\n",  SERVICE, serverURI));

        } catch( Exception e) {
            Log.severe(e.getMessage());
        }
    }


}
