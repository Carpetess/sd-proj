package fctreddit.api.clients.ContentClients;

import fctreddit.api.java.Result;
import fctreddit.impl.grpc.generated_java.ContentGrpc;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf;
import fctreddit.impl.grpc.generated_java.ImageProtoBuf;
import io.grpc.Channel;
import io.grpc.LoadBalancerRegistry;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.internal.PickFirstLoadBalancerProvider;

import java.net.URI;

import static fctreddit.api.util.ErrorParser.statusToErrorCode;

public class ContentGrpcClient extends ContentClient {
    final ContentGrpc.ContentBlockingStub stub;

    static {
        LoadBalancerRegistry.getDefaultRegistry().register(new PickFirstLoadBalancerProvider());
    }

    public ContentGrpcClient(URI selectedServiceURI) {
        Channel channel = ManagedChannelBuilder.forAddress(selectedServiceURI.getHost(), selectedServiceURI.getPort())
                .enableRetry().usePlaintext().build();
        stub = ContentGrpc.newBlockingStub(channel);

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

