package fctreddit.impl.client.grpc;

import fctreddit.api.java.Image;
import fctreddit.api.java.Result;
import fctreddit.impl.client.ImageClient;
import fctreddit.impl.grpc.generated_java.ContentGrpc;
import fctreddit.impl.grpc.generated_java.ImageGrpc;
import fctreddit.impl.grpc.generated_java.ImageProtoBuf;
import io.grpc.Channel;
import io.grpc.LoadBalancerRegistry;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.internal.PickFirstLoadBalancerProvider;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;

import static fctreddit.impl.server.ErrorParser.statusToErrorCode;

public class ImageGrpcClient extends ImageClient implements Image {
    final ImageGrpc.ImageBlockingStub stub;

    static {
        LoadBalancerRegistry.getDefaultRegistry().register(new PickFirstLoadBalancerProvider());
    }


    public ImageGrpcClient(URI selectedServiceURI) {
        super(selectedServiceURI);
        String trustStoreFilename = System.getProperty("javax.net.ssl.trustStore");
        String trustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword");

        try (FileInputStream input = new FileInputStream(trustStoreFilename)) {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(input, trustStorePassword.toCharArray());
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());

            trustManagerFactory.init(trustStore);

            SslContext sslContext = GrpcSslContexts
                    .configure(
                            SslContextBuilder.forClient().trustManager(trustManagerFactory)
                    ).build();

            Channel channel = io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
                    .forAddress(selectedServiceURI.getHost(), selectedServiceURI.getPort())
                    .sslContext(sslContext)
                    .enableRetry()
                    .build();

            stub = ImageGrpc.newBlockingStub(channel);
        } catch (Exception e) {
            throw new RuntimeException("Could not load trust store", e);
        }
    }

    @Override
    public Result<String> createImage(String userId, byte[] imageContents, String password) throws IOException {
        return null;
    }

    @Override
    public Result<byte[]> getImage(String userId, String imageId) throws IOException {
        return null;
    }

    public Result<Void> deleteImage(String userId, String imageId, String password) {
        try {
            ImageProtoBuf.DeleteImageResult res = stub.deleteImage(ImageProtoBuf.DeleteImageArgs.newBuilder()
                    .setImageId(imageId).setUserId(userId).setPassword(password).build());
            return Result.ok();
        } catch (StatusRuntimeException sre) {
            return Result.error(statusToErrorCode(sre.getStatus()));
        }
    }
}