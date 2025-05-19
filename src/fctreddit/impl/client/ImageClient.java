package fctreddit.impl.client;

import fctreddit.api.java.Image;
import fctreddit.impl.client.grpc.ImageGrpcClient;
import fctreddit.impl.client.rest.ImageRestClient;

import java.net.URI;

public abstract class ImageClient extends Client implements Image {
		
	public ImageClient(URI serverURI) {
		super(serverURI);
	}

	public static ImageClient getImageClient(URI serverURI) {
		if(serverURI.toString().endsWith("/rest")) {
			return new ImageRestClient(serverURI);
		} else {
			return new ImageGrpcClient(serverURI);
		}
	}
}
