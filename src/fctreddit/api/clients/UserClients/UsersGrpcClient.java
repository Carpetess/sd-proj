package fctreddit.api.clients.UserClients;

import fctreddit.api.data.User;
import fctreddit.api.grpc.DataModelAdaptor;
import fctreddit.api.java.Result;
import fctreddit.impl.grpc.generated_java.UsersGrpc;
import fctreddit.impl.grpc.generated_java.UsersProtoBuf;
import io.grpc.Channel;
import io.grpc.LoadBalancerRegistry;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.internal.PickFirstLoadBalancerProvider;

import java.net.URI;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static fctreddit.api.util.ErrorParser.statusToErrorCode;

public class UsersGrpcClient extends UsersClient {

    static {
        LoadBalancerRegistry.getDefaultRegistry().register(new PickFirstLoadBalancerProvider());
    }

    final UsersGrpc.UsersBlockingStub stub;

    public UsersGrpcClient(URI selectedServiceURI) {
        Channel channel = ManagedChannelBuilder.forAddress(selectedServiceURI.getHost(), selectedServiceURI.getPort())
                .enableRetry().usePlaintext().build();
        stub = UsersGrpc.newBlockingStub(channel);

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
