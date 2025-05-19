package fctreddit.impl.server.grpc;

import fctreddit.impl.server.java.JavaServer;
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


    public static void main(String[] args) throws Exception {
        String keyStoreFileName = System.getProperty("javax.net.ssl.keyStore");
        String keyStorePassword = System.getProperty("javax.net.ssl.keyStorePassword");

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

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

        String serverURI = String.format(SERVER_URI_FMT, InetAddress.getLocalHost().getHostName(), PORT);

        discovery = new Discovery(Discovery.DISCOVERY_ADDR, SERVICE, serverURI);
        JavaServer.setDiscovery(discovery);
        discovery.start();

        Log.info(String.format("%s Server ready @ %s\n",  SERVICE, serverURI));
        server.start().awaitTermination();
    }
}