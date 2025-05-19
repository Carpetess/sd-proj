package fctreddit.impl.server.rest;

import fctreddit.impl.server.Discovery;
import fctreddit.impl.server.SecretKeeper;
import fctreddit.impl.server.java.JavaServer;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import javax.net.ssl.SSLContext;
import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;

public class ContentServer {

    private static Logger Log = Logger.getLogger(UsersServer.class.getName());

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    private static Discovery discovery;

    public static int PORT = 8080;
    public static final String SERVICE = "Content";
    private static final String SERVER_URI_FMT = "https://%s:%s/rest";


    public static void main(String[] args) {

        try {
            ResourceConfig config = new ResourceConfig();
            config.register(ContentResource.class);
            SecretKeeper.getInstance().setSecret(args[args.length-1]);
            String hostName = InetAddress.getLocalHost().getHostName();
            String serverURI = String.format(SERVER_URI_FMT, hostName, PORT);
            discovery = new Discovery(Discovery.DISCOVERY_ADDR, SERVICE, serverURI);
            JavaServer.setDiscovery(discovery);
            discovery.start();
            JdkHttpServerFactory.createHttpServer( URI.create(serverURI), config, SSLContext.getDefault());

            Log.info(String.format("%s Server ready @ %s\n",  SERVICE, serverURI));

        } catch( Exception e) {
            Log.severe(e.getMessage());
        }
    }

}
