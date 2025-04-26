package fctreddit.api.server.grpc;

import fctreddit.api.clients.clientFactories.UserClientFactory;
import fctreddit.api.grpc.GrpcImageServerStub;
import fctreddit.api.rest.ImageResource;
import fctreddit.api.server.rest.UsersServer;
import fctreddit.api.server.serviceDiscovery.Discovery;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.ServerCredentials;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;

public class ImageServer {

    private static Logger Log = Logger.getLogger(UsersServer.class.getName());

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    private static Discovery discovery;

    public static int PORT = 8081;
    public static final String SERVICE = "Image";
    private static final String SERVER_URI_FMT = "http://%s:%s/grpc";
    public static String serverURI;


    public static void main(String[] args) {

        try {
            GrpcImageServerStub stub = new GrpcImageServerStub();
            ServerCredentials cred = InsecureServerCredentials.create();
            Server server = Grpc.newServerBuilderForPort(PORT, cred).addService(stub).build();
            serverURI = String.format(SERVER_URI_FMT, InetAddress.getLocalHost().getHostAddress(), PORT);

            discovery = new Discovery(Discovery.DISCOVERY_ADDR, SERVICE, serverURI);
            discovery.start();
            UserClientFactory.getInstance().setDiscovery(discovery);
            Log.info(String.format("%s Server ready @ %s\n",  SERVICE, serverURI));
            server.start().awaitTermination();
        } catch( Exception e) {
            Log.severe(e.getMessage());
        }
    }

}