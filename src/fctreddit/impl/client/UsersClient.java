package fctreddit.impl.client;

import fctreddit.api.java.Users;
import fctreddit.impl.client.grpc.UsersGrpcClient;
import fctreddit.impl.client.rest.UsersRestClient;

import java.net.URI;

public abstract class UsersClient extends Client implements Users {

	public UsersClient(URI serverURI) {
		super(serverURI);
	}

	public static UsersClient getUsersClient(URI serverURI) {
		if(serverURI.toString().endsWith("/rest")) {
			return new UsersRestClient(serverURI);
		} else {
			return new UsersGrpcClient(serverURI);
		}
	}
	
}
