package fctreddit.api.grpc;

import com.google.protobuf.ByteString;
import fctreddit.api.java.Result;
import fctreddit.api.java.resources.ImageJava;
import fctreddit.api.server.grpc.ImageServer;
import fctreddit.impl.grpc.generated_java.ImageGrpc;
import fctreddit.impl.grpc.generated_java.ImageProtoBuf;
import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;

import java.net.URI;

import static fctreddit.api.util.ErrorParser.errorCodeToThrowable;

public class GrpcImageServerStub implements ImageGrpc.AsyncService, BindableService {
    private ImageJava impl = new ImageJava();

    @Override
    public ServerServiceDefinition bindService() {
        return ImageGrpc.bindService(this);
    }

    @Override
    public void createImage(ImageProtoBuf.CreateImageArgs request, StreamObserver<ImageProtoBuf.CreateImageResult> responseObserver) {
        Result<String> res = impl.createImage(request.getUserId(), request.getImageContents().toByteArray(), request.hasPassword() ? request.getPassword() : null);
        if(!res.isOK())
            responseObserver.onError(errorCodeToThrowable(res.error()));

        URI finalURI = URI.create(ImageServer.serverURI + res.value());
        responseObserver.onNext(ImageProtoBuf.CreateImageResult.newBuilder().setImageId(finalURI.toString()).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getImage(ImageProtoBuf.GetImageArgs request, StreamObserver<ImageProtoBuf.GetImageResult> responseObserver) {
        Result<byte[]> res = impl.getImage(request.getUserId(), request.getImageId());
        if(!res.isOK())
            responseObserver.onError(errorCodeToThrowable(res.error()));
        ByteString bs = ByteString.copyFrom(res.value());
        responseObserver.onNext(ImageProtoBuf.GetImageResult.newBuilder().setData(bs).build());
        responseObserver.onCompleted();
    }

    @Override
    public void deleteImage(ImageProtoBuf.DeleteImageArgs request, StreamObserver<ImageProtoBuf.DeleteImageResult> responseObserver) {
        Result<Void> res = impl.deleteImage(request.getUserId(), request.getImageId(), request.hasPassword() ? request.getPassword() : null);
        if(!res.isOK())
            responseObserver.onError(errorCodeToThrowable(res.error()));
        responseObserver.onNext(ImageProtoBuf.DeleteImageResult.newBuilder().build());
        responseObserver.onCompleted();
    }


}
