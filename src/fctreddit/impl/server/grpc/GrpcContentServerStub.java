package fctreddit.impl.server.grpc;

import fctreddit.api.data.Post;
import fctreddit.api.java.Result;
import fctreddit.impl.server.java.ContentJava;
import fctreddit.impl.grpc.generated_java.ContentGrpc;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf;
import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;

import java.util.List;

import static fctreddit.impl.server.ErrorParser.errorCodeToThrowable;

public class GrpcContentServerStub implements ContentGrpc.AsyncService, BindableService {
    private final ContentJava impl = new ContentJava();

    @Override
    public ServerServiceDefinition bindService() {
        return ContentGrpc.bindService(this);
    }

    @Override
    public void createPost(ContentProtoBuf.CreatePostArgs request, StreamObserver<ContentProtoBuf.CreatePostResult> responseObserver) {
        Result<String> res = impl.createPost(DataModelAdaptor.GrpcPost_to_Post(request.getPost()), request.getPassword());
        if(!res.isOK())
            responseObserver.onError(errorCodeToThrowable(res.error()));
        responseObserver.onNext(ContentProtoBuf.CreatePostResult.newBuilder().setPostId(res.value()).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getPost(ContentProtoBuf.GetPostArgs request, StreamObserver<ContentProtoBuf.GrpcPost> responseObserver) {
        Result<Post> res = impl.getPost(request.getPostId());
        if (!res.isOK())
            responseObserver.onError(errorCodeToThrowable(res.error()));
        responseObserver.onNext(DataModelAdaptor.Post_to_GrpcPost(res.value()));
        responseObserver.onCompleted();
    }

    @Override
    public void deletePost(ContentProtoBuf.DeletePostArgs request, StreamObserver<ContentProtoBuf.EmptyMessage> responseObserver) {
        Result<Void> res = impl.deletePost(request.getPostId(), request.hasPassword() ? request.getPassword() : null);
        if (!res.isOK())
            responseObserver.onError(errorCodeToThrowable(res.error()));
        responseObserver.onNext(ContentProtoBuf.EmptyMessage.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void downVotePost(ContentProtoBuf.ChangeVoteArgs request, StreamObserver<ContentProtoBuf.EmptyMessage> responseObserver) {
        Result<Void> res = impl.downVotePost(request.getPostId(), request.getUserId(), request.hasPassword() ? request.getPassword() : null);
        if(!res.isOK())
            responseObserver.onError(errorCodeToThrowable(res.error()));
        responseObserver.onNext(ContentProtoBuf.EmptyMessage.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void upVotePost(ContentProtoBuf.ChangeVoteArgs request, StreamObserver<ContentProtoBuf.EmptyMessage> responseObserver) {
        Result<Void> res = impl.upVotePost(request.getPostId(), request.getUserId(), request.hasPassword() ? request.getPassword() : null);
        if(!res.isOK())
            responseObserver.onError(errorCodeToThrowable(res.error()));
        responseObserver.onNext(ContentProtoBuf.EmptyMessage.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void removeDownVotePost(ContentProtoBuf.ChangeVoteArgs request, StreamObserver<ContentProtoBuf.EmptyMessage> responseObserver) {
        Result<Void> res = impl.removeDownVotePost(request.getPostId(), request.getUserId(), request.hasPassword() ? request.getPassword() : null);
        if(!res.isOK())
            responseObserver.onError(errorCodeToThrowable(res.error()));
        responseObserver.onNext(ContentProtoBuf.EmptyMessage.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void removeUpVotePost(ContentProtoBuf.ChangeVoteArgs request, StreamObserver<ContentProtoBuf.EmptyMessage> responseObserver) {
        Result<Void> res = impl.removeUpVotePost(request.getPostId(), request.getUserId(), request.hasPassword() ? request.getPassword() : null);
        if(!res.isOK())
            responseObserver.onError(errorCodeToThrowable(res.error()));
        responseObserver.onNext(ContentProtoBuf.EmptyMessage.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void getDownVotes(ContentProtoBuf.GetPostArgs request, StreamObserver<ContentProtoBuf.VoteCountResult> responseObserver) {
        Result<Integer> res = impl.getDownVotes(request.getPostId());
        if(!res.isOK())
            responseObserver.onError(errorCodeToThrowable(res.error()));
        responseObserver.onNext(ContentProtoBuf.VoteCountResult.newBuilder().setCount(res.value()).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getUpVotes(ContentProtoBuf.GetPostArgs request, StreamObserver<ContentProtoBuf.VoteCountResult> responseObserver) {
        Result<Integer> res = impl.getupVotes(request.getPostId());
        if (!res.isOK())
            responseObserver.onError(errorCodeToThrowable(res.error()));
        responseObserver.onNext(ContentProtoBuf.VoteCountResult.newBuilder().setCount(res.value()).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getPosts(ContentProtoBuf.GetPostsArgs request, StreamObserver<ContentProtoBuf.GetPostsResult> responseObserver) {
        Result<List<String>> res = impl.getPosts(request.hasTimestamp() ? request.getTimestamp() : 0, request.hasSortOrder() ? request.getSortOrder() : null);
        if(!res.isOK())
            responseObserver.onError(errorCodeToThrowable(res.error()));
        responseObserver.onNext(ContentProtoBuf.GetPostsResult.newBuilder().addAllPostId(res.value()).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getPostAnswers(ContentProtoBuf.GetPostAnswersArgs request, StreamObserver<ContentProtoBuf.GetPostsResult> responseObserver) {
        Result<List<String>> res = impl.getPostAnswers(request.getPostId(), request.hasTimeout() ? request.getTimeout() : 0L);
        if(!res.isOK())
            responseObserver.onError(errorCodeToThrowable(res.error()));
        responseObserver.onNext(ContentProtoBuf.GetPostsResult.newBuilder().addAllPostId(res.value()).build());
        responseObserver.onCompleted();
    }

    @Override
    public void updatePost(ContentProtoBuf.UpdatePostArgs request, StreamObserver<ContentProtoBuf.GrpcPost> responseObserver) {
        Result<Post> res = impl.updatePost(request.getPostId(), request.hasPassword() ? request.getPassword() : null, DataModelAdaptor.GrpcPost_to_Post(request.getPost()));
        if(!res.isOK())
            responseObserver.onError(errorCodeToThrowable(res.error()));
        responseObserver.onNext(DataModelAdaptor.Post_to_GrpcPost(res.value()));
        responseObserver.onCompleted();
    }

    @Override
    public void removeAllUserVotes(ContentProtoBuf.RemoveAllUserVoteArgs request, StreamObserver<ContentProtoBuf.EmptyMessage> responseObserver) {
        Result<Void> res = impl.removeAllUserVotes(request.getUserId(), request.getPassword());
        if(!res.isOK())
            responseObserver.onError(errorCodeToThrowable(res.error()));
        responseObserver.onNext(ContentProtoBuf.EmptyMessage.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void updatePostOwner(ContentProtoBuf.UpdatePostOwnerArgs request, StreamObserver<ContentProtoBuf.EmptyMessage> responseObserver) {
        Result<Void> res = impl.updatePostOwner(request.getUserId(), request.getPassword());
        if(!res.isOK())
            responseObserver.onError(errorCodeToThrowable(res.error()));
        responseObserver.onNext(ContentProtoBuf.EmptyMessage.newBuilder().build());
        responseObserver.onCompleted();

    }

}
