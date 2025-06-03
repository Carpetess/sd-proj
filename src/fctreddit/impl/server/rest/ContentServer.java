package fctreddit.impl.server.rest;

import fctreddit.api.java.Content;
import fctreddit.api.java.Image;
import fctreddit.impl.server.Discovery;
import fctreddit.impl.server.SecretKeeper;
import fctreddit.impl.server.java.ContentJava;
import fctreddit.impl.server.java.JavaServer;
import fctreddit.impl.server.kafka.KafkaPublisher;
import fctreddit.impl.server.kafka.KafkaSubscriber;
import fctreddit.impl.server.kafka.KafkaUtils;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import javax.net.ssl.SSLContext;
import java.net.InetAddress;
import java.net.URI;
import java.util.List;
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
        KafkaUtils.createTopic(Image.IMAGE_REFERENCE_COUNTER_TOPIC);
        KafkaPublisher publisher = KafkaPublisher.createPublisher("kafka:9092");
        KafkaUtils.createTopic(Image.DELETED_IMAGE_TOPIC);
        KafkaSubscriber subscriber = KafkaSubscriber.createSubscriber("kafka:9092", List.of(Image.DELETED_IMAGE_TOPIC));
        ContentJava java = new ContentJava();
        java.setPublisher(publisher);
        java.setSubscriber(subscriber);
        try {
            ResourceConfig config = new ResourceConfig();
            config.register(ContentResource.class);
            SecretKeeper.getInstance().setSecret(args[args.length - 1]);
            String hostName = InetAddress.getLocalHost().getHostName();
            String serverURI = String.format(SERVER_URI_FMT, hostName, PORT);
            discovery = new Discovery(Discovery.DISCOVERY_ADDR, SERVICE, serverURI);
            discovery.start();
            JavaServer.setDiscovery(discovery);
            JdkHttpServerFactory.createHttpServer(URI.create(serverURI), config, SSLContext.getDefault());

            Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));

        } catch (Exception e) {
            Log.severe(e.getMessage());
        }
    }

}
