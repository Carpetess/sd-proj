package fctreddit.api.clients.ContentClients;

import fctreddit.api.java.Result;
import fctreddit.impl.grpc.generated_java.ContentGrpc;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.net.URI;

public class ContentGrpcClient extends ContentClient {
    final ContentGrpc.ContentStub stub;

    public ContentGrpcClient(URI selectedServiceURI) {
        Channel channel = ManagedChannelBuilder.forAddress(selectedServiceURI.getHost(), selectedServiceURI.getPort())
                .enableRetry().usePlaintext().build();
        stub = ContentGrpc.newStub(channel);
    }

    @Override
    public Result<Void> removeAllUserVotes(String userId, String password) {
        return null;
    }

    @Override
    public Result<Void> updatePostOwner(String authorId, String password) {
        return null;
    }
}
