package fctreddit.impl.server.grpc;

import fctreddit.api.java.Users;
import fctreddit.impl.server.Discovery;
import fctreddit.impl.server.SecretKeeper;
import fctreddit.impl.server.java.JavaServer;
import fctreddit.impl.server.java.UsersJava;
import io.grpc.Server;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.KeyManagerFactory;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.security.KeyStore;
import java.util.logging.Logger;

public class UsersServer {

    private static Logger Log = Logger.getLogger(UsersServer.class.getName());

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    private static Discovery discovery;

    public static int PORT = 8081;
    public static final String SERVICE = "Users";
    private static final String SERVER_URI_FMT = "grpc://%s:%s/grpc";


    public static void main(String[] args) throws Exception {
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

        GrpcUsersServerStub stub = new GrpcUsersServerStub();

        SslContext context = GrpcSslContexts.configure(
                SslContextBuilder.forServer(keyManagerFactory)
        ).build();

        Server server = NettyServerBuilder.forPort(PORT)
                .addService(stub).sslContext(context).build();

        String serverURI = String.format(SERVER_URI_FMT, InetAddress.getLocalHost().getHostName(), PORT);

        discovery = new Discovery(Discovery.DISCOVERY_ADDR, SERVICE, serverURI);
        discovery.start();
        JavaServer.setDiscovery(discovery);

        Log.info(String.format("%s Server ready @ %s\n",  SERVICE, serverURI));
        server.start().awaitTermination();
    }
}