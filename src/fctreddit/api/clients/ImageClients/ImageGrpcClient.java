package fctreddit.api.clients.ImageClients;

import fctreddit.api.grpc.DataModelAdaptor;
import fctreddit.api.java.Result;
import fctreddit.impl.grpc.generated_java.ImageGrpc;
import fctreddit.impl.grpc.generated_java.ImageProtoBuf;
import fctreddit.impl.grpc.generated_java.UsersGrpc;
import fctreddit.impl.grpc.generated_java.UsersProtoBuf;
import io.grpc.Channel;
import io.grpc.LoadBalancerRegistry;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.internal.PickFirstLoadBalancerProvider;

import java.net.URI;

import static fctreddit.api.util.ErrorParser.statusToErrorCode;

public class ImageGrpcClient extends ImageClient {
    final ImageGrpc.ImageBlockingStub stub;

    static {
        LoadBalancerRegistry.getDefaultRegistry().register(new PickFirstLoadBalancerProvider());
    }


    public ImageGrpcClient(URI selectedServiceURI) {
        Channel channel = ManagedChannelBuilder.forAddress(selectedServiceURI.getHost(), selectedServiceURI.getPort())
                .enableRetry().usePlaintext().build();
        stub = ImageGrpc.newBlockingStub(channel);

    }

    @Override
    public Result<Void> deleteImage(String userId, String password, String imageId) {
        try {
            ImageProtoBuf.DeleteImageResult res = stub.deleteImage(ImageProtoBuf.DeleteImageArgs.newBuilder()
                    .setImageId(imageId).setUserId(userId).setPassword(password).build());
            return Result.ok();
        } catch (StatusRuntimeException sre) {
            return Result.error(statusToErrorCode(sre.getStatus()));
        }
    }
}
