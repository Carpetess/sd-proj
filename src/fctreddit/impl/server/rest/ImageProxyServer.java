package fctreddit.impl.server.rest;

import fctreddit.api.java.Result;
import fctreddit.impl.server.Discovery;
import fctreddit.impl.server.SecretKeeper;
import fctreddit.impl.server.java.ImageProxy.ImageProxyJava;
import fctreddit.impl.server.java.JavaServer;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import javax.net.ssl.SSLContext;
import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;

import static java.lang.System.exit;

public class ImageProxyServer {


    private static Logger Log = Logger.getLogger(UsersServer.class.getName());

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    private static Discovery discovery;

    public static int PORT = 8080;
    public static final String SERVICE = "Image";
    private static final String SERVER_URI_FMT = "https://%s:%s/rest";
    public static String serverURI;
    private static String associatedAlbumId;
    private static String hostName;


    public static void main(String[] args) {
        associatedAlbumId = null;
        boolean reboot = Boolean.parseBoolean(args[0]);

        try {
            ResourceConfig config = new ResourceConfig();
            config.register(ImageProxyResource.class);
            Log.info("Starting Content Server\n");
            Log.info("Using Secret: " + args[args.length-1] + "\n");
            SecretKeeper.getInstance().setSecret(args[args.length - 1]);
            Log.info("Secret registered: " + SecretKeeper.getInstance().getSecret() + "\n");
            hostName = InetAddress.getLocalHost().getHostName();
            startImgurAlbum(reboot);
            serverURI = String.format(SERVER_URI_FMT, hostName, PORT);
            discovery = new Discovery(Discovery.DISCOVERY_ADDR, SERVICE, serverURI);
            discovery.start();
            JavaServer.setDiscovery(discovery);
            JdkHttpServerFactory.createHttpServer(URI.create(serverURI), config, SSLContext.getDefault());

            Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));

        } catch (Exception e) {
            Log.severe(e.getMessage());
        }
    }

    public static String getServerURI() {
        return serverURI;
    }

    public static String getAssociatedAlbumId() {
        return associatedAlbumId;
    }

    private static void startImgurAlbum(boolean reboot) {
        Log.info("Starting Imgur Album\n");
        ImageProxyJava impl = new ImageProxyJava();

        Result<String> res = impl.createAlbum(hostName);
        Log.info("Imgur Album created with id: " + res.value() + "\n");
        associatedAlbumId = res.value();
        if(reboot){
            Log.info("Rebooting Imgur Album\n");
            impl.deleteAlbum(associatedAlbumId);
            Log.info("Imgur Album rebooted with id: " + associatedAlbumId + "\n");
        }


    }
}
