package fctreddit.impl.client.grpc;

import fctreddit.api.data.User;
import fctreddit.impl.client.UsersClient;
import fctreddit.impl.server.grpc.DataModelAdaptor;
import fctreddit.api.java.Result;
import fctreddit.api.java.Users;
import fctreddit.impl.grpc.generated_java.UsersGrpc;
import fctreddit.impl.grpc.generated_java.UsersProtoBuf;
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
import java.net.URI;
import java.security.KeyStore;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static fctreddit.impl.server.ErrorParser.statusToErrorCode;

public class UsersGrpcClient extends UsersClient implements Users {

    static {
        LoadBalancerRegistry.getDefaultRegistry().register(new PickFirstLoadBalancerProvider());
    }

    final UsersGrpc.UsersBlockingStub stub;

    public UsersGrpcClient(URI selectedServiceURI) {
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

            stub = UsersGrpc.newBlockingStub(channel);
        } catch (Exception e) {
            throw new RuntimeException("Could not load trust store", e);
        }
    }

    @Override
    public Result<String> createUser(User user) {
        try {
            UsersProtoBuf.CreateUserResult res = stub.createUser(UsersProtoBuf.CreateUserArgs.newBuilder()
                    .setUser(DataModelAdaptor.User_to_GrpcUser(user)).build());
            return Result.ok(res.getUserId());
        } catch (StatusRuntimeException sre) {
            return Result.error(statusToErrorCode(sre.getStatus()));
        }
    }

    @Override
    public Result<User> getUser(String userId, String password) {
        try {
            UsersProtoBuf.GetUserResult res = stub.getUser(UsersProtoBuf.GetUserArgs.newBuilder()
                    .setUserId(userId)
                    .setPassword(password)
                    .build());
            return Result.ok(DataModelAdaptor.GrpcUser_to_User(res.getUser()) );
        } catch (StatusRuntimeException sre) {
            return Result.error(statusToErrorCode(sre.getStatus()));
        }
    }

    @Override
    public Result<User> updateUser(String userId, String password, User user) {
            try {
            UsersProtoBuf.UpdateUserResult res = stub.updateUser(UsersProtoBuf.UpdateUserArgs.newBuilder()
                    .setUserId(userId).setPassword(password).setUser(DataModelAdaptor.User_to_GrpcUser(user)).build());
            return Result.ok(DataModelAdaptor.GrpcUser_to_User(res.getUser()) );
        } catch (StatusRuntimeException sre) {
            return Result.error(statusToErrorCode(sre.getStatus()));
        }
    }


    @Override
    public Result<User> deleteUser(String userId, String password) {
        try {
            UsersProtoBuf.DeleteUserResult res = stub.deleteUser(UsersProtoBuf.DeleteUserArgs.newBuilder()
                    .setUserId(userId).setPassword(password).build());
            return Result.ok(DataModelAdaptor.GrpcUser_to_User(res.getUser()) );
        } catch (StatusRuntimeException sre) {
            return Result.error(statusToErrorCode(sre.getStatus()));
        }
    }

    @Override
    public Result<List<User>> searchUsers(String pattern) {
        try {
            Iterator<UsersProtoBuf.GrpcUser> iterator = stub.searchUsers(UsersProtoBuf.SearchUserArgs.newBuilder()
                    .setPattern(pattern).build());
            List<User> ret = new LinkedList<>();
            while (iterator.hasNext()) {
                ret.add(DataModelAdaptor.GrpcUser_to_User(iterator.next()));
            }
            return Result.ok(ret);
        } catch (StatusRuntimeException sre) {
            return Result.error(statusToErrorCode(sre.getStatus()));
        }
    }

}
