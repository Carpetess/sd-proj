package fctreddit.impl.server.grpc;

import fctreddit.api.java.Image;
import fctreddit.impl.server.SecretKeeper;
import fctreddit.impl.server.java.ContentJava;
import fctreddit.impl.server.java.ImageJava;
import fctreddit.impl.server.java.JavaServer;
import fctreddit.impl.server.kafka.KafkaPublisher;
import fctreddit.impl.server.kafka.KafkaSubscriber;
import fctreddit.impl.server.kafka.KafkaUtils;
import fctreddit.impl.server.rest.UsersServer;
import fctreddit.impl.server.Discovery;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.ServerCredentials;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.KeyManagerFactory;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.security.KeyStore;
import java.util.List;
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
    private static final String SERVER_URI_FMT = "grpc://%s:%s/grpc";
    public static String serverURI;


    public static void main(String[] args) throws Exception {
        KafkaUtils.createTopic(Image.IMAGE_REFERENCE_COUNTER_TOPIC);
        KafkaPublisher publisher = KafkaPublisher.createPublisher("kafka:9092");
        KafkaUtils.createTopic(Image.DELETED_IMAGE_TOPIC);
        KafkaSubscriber subscriber = KafkaSubscriber.createSubscriber("kafka:9092", List.of(Image.DELETED_IMAGE_TOPIC));
        ImageJava.setPublisher(publisher);
        ImageJava.setSubscriber(subscriber);

        String keyStoreFileName = System.getProperty("javax.net.ssl.keyStore");
        String keyStorePassword = System.getProperty("javax.net.ssl.keyStorePassword");

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        SecretKeeper.getInstance().setSecret(args[args.length-1]);
        try (FileInputStream fis = new FileInputStream(keyStoreFileName)) {
            keyStore.load(fis, keyStorePassword.toCharArray());
        }

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());

        GrpcImageServerStub stub = new GrpcImageServerStub();

        SslContext context = GrpcSslContexts.configure(
                SslContextBuilder.forServer(keyManagerFactory)
        ).build();

        Server server = NettyServerBuilder.forPort(PORT)
                .addService(stub).sslContext(context).build();

        serverURI = String.format(SERVER_URI_FMT, InetAddress.getLocalHost().getHostName(), PORT);

        discovery = new Discovery(Discovery.DISCOVERY_ADDR, SERVICE, serverURI);
        discovery.start();
        JavaServer.setDiscovery(discovery);

        Log.info(String.format("%s Server ready @ %s\n",  SERVICE, serverURI));
        server.start().awaitTermination();
    }
}