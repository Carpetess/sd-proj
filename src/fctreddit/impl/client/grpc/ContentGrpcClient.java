package fctreddit.impl.client.grpc;

import fctreddit.api.data.Post;
import fctreddit.api.java.Content;
import fctreddit.api.java.Result;
import fctreddit.impl.client.ContentClient;
import fctreddit.impl.grpc.generated_java.ContentGrpc;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf;
import io.grpc.Channel;
import io.grpc.LoadBalancerRegistry;
import io.grpc.StatusRuntimeException;
import io.grpc.internal.PickFirstLoadBalancerProvider;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.net.URI;
import java.security.KeyStore;
import java.util.List;

import static fctreddit.impl.server.ErrorParser.statusToErrorCode;

public class ContentGrpcClient extends ContentClient implements Content {
    final ContentGrpc.ContentBlockingStub stub;

    static {
        LoadBalancerRegistry.getDefaultRegistry().register(new PickFirstLoadBalancerProvider());
    }

    public ContentGrpcClient(URI selectedServiceURI) {
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

            stub = ContentGrpc.newBlockingStub(channel);
        } catch (Exception e) {
            throw new RuntimeException("Could not load trust store", e);
        }
    }

    @Override
    public Result<Void> removeAllUserVotes(String userId, String password) {
        try {
            stub.removeAllUserVotes(ContentProtoBuf.RemoveAllUserVoteArgs.newBuilder()
                    .setUserId(userId).setPassword(password).build());
            return Result.ok();
        } catch (StatusRuntimeException sre) {
            return Result.error(statusToErrorCode(sre.getStatus()));
        }
    }

    @Override
    public Result<String> createPost(Post post, String userPassword) {
        return null;
    }

    @Override
    public Result<List<String>> getPosts(long timestamp, String sortOrder) {
        return null;
    }

    @Override
    public Result<Post> getPost(String postId) {
        return null;
    }

    @Override
    public Result<List<String>> getPostAnswers(String postId, long maxTimeout) {
        return null;
    }

    @Override
    public Result<Post> updatePost(String postId, String userPassword, Post post) {
        return null;
    }

    @Override
    public Result<Void> deletePost(String postId, String userPassword) {
        return null;
    }

    @Override
    public Result<Void> upVotePost(String postId, String userId, String userPassword) {
        return null;
    }

    @Override
    public Result<Void> removeUpVotePost(String postId, String userId, String userPassword) {
        return null;
    }

    @Override
    public Result<Void> downVotePost(String postId, String userId, String userPassword) {
        return null;
    }

    @Override
    public Result<Void> removeDownVotePost(String postId, String userId, String userPassword) {
        return null;
    }

    @Override
    public Result<Integer> getupVotes(String postId) {
        return null;
    }

    @Override
    public Result<Integer> getDownVotes(String postId) {
        return null;
    }

    @Override
    public Result<Void> updatePostOwner(String authorId, String password) {
        try {
            stub.updatePostOwner(ContentProtoBuf.UpdatePostOwnerArgs.newBuilder()
                    .setUserId(authorId).setPassword(password).build());
            return Result.ok();
        } catch (StatusRuntimeException sre) {
            return Result.error(statusToErrorCode(sre.getStatus()));
        }
    }
}

