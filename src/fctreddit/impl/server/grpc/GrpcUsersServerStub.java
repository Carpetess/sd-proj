package fctreddit.impl.server.grpc;

import fctreddit.api.data.User;
import fctreddit.api.java.Result;
import fctreddit.api.java.Users;
import fctreddit.impl.server.java.UsersJava;
import fctreddit.impl.grpc.generated_java.UsersGrpc;
import fctreddit.impl.grpc.generated_java.UsersProtoBuf.*;
import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;

import java.util.List;

import static fctreddit.impl.server.ErrorParser.errorCodeToThrowable;


public class GrpcUsersServerStub implements UsersGrpc.AsyncService, BindableService {
    Users impl = new UsersJava();


    @Override
    public ServerServiceDefinition bindService() {
        return UsersGrpc.bindService(this);
    }

    public void createUser(CreateUserArgs request, StreamObserver<CreateUserResult> responseObserver) {
        Result<String> res = impl.createUser(DataModelAdaptor.GrpcUser_to_User(request.getUser()));
        if (!res.isOK())
            responseObserver.onError(errorCodeToThrowable(res.error()));

        responseObserver.onNext(CreateUserResult.newBuilder().setUserId(res.value()).build());
        responseObserver.onCompleted();
    }

    public void getUser(GetUserArgs request, StreamObserver<GetUserResult> responseObserver) {
        Result<User> res = impl.getUser(request.getUserId(), request.hasPassword() ? request.getPassword() : null);
        if (!res.isOK())
            responseObserver.onError(errorCodeToThrowable(res.error()));

        responseObserver.onNext(GetUserResult.newBuilder().setUser(DataModelAdaptor.User_to_GrpcUser(res.value())).build());
        responseObserver.onCompleted();
    }

    public void updateUser(UpdateUserArgs request, StreamObserver<UpdateUserResult> responseObserver) {
        Result<User> res = impl.updateUser(request.getUserId(), request.getPassword(), DataModelAdaptor.GrpcUser_to_User(request.getUser()));
        if (!res.isOK())
            responseObserver.onError(errorCodeToThrowable(res.error()));

        responseObserver.onNext(UpdateUserResult.newBuilder().setUser(DataModelAdaptor.User_to_GrpcUser(res.value())).build());
        responseObserver.onCompleted();
    }

    public void deleteUser(DeleteUserArgs request, StreamObserver<DeleteUserResult> responseObserver) {
        Result<User> res = impl.deleteUser(request.getUserId(), request.getPassword());
        if (!res.isOK())
            responseObserver.onError(errorCodeToThrowable(res.error()));

        responseObserver.onNext(DeleteUserResult.newBuilder().setUser(DataModelAdaptor.User_to_GrpcUser(res.value())).build());
        responseObserver.onCompleted();
    }

    public void searchUsers(SearchUserArgs request, StreamObserver<GrpcUser> responseObserver) {
        Result<List<User>> res = impl.searchUsers(request.hasPattern() ? request.getPattern() : null);
        if (!res.isOK())
            responseObserver.onError(errorCodeToThrowable(res.error()));

        for (User u : res.value()) {
            responseObserver.onNext(DataModelAdaptor.User_to_GrpcUser(u));
        }
        responseObserver.onCompleted();
    }

}
