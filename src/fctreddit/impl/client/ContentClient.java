package fctreddit.impl.client;

import fctreddit.api.java.Content;
import fctreddit.impl.client.grpc.ContentGrpcClient;
import fctreddit.impl.client.rest.ContentRestClient;

import java.net.URI;

public abstract class ContentClient extends Client implements Content {
	
	public ContentClient(URI serverURI) {
		super(serverURI);
	}

	public static ContentClient getContentClient(URI serverURI) {
		if(serverURI.toString().endsWith("/rest")) {
			return new ContentRestClient(serverURI);
		} else {
			return new ContentGrpcClient(serverURI);
		}
	}
}
