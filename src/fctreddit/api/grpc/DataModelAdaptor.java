package fctreddit.api.grpc;

import fctreddit.api.data.Post;
import fctreddit.api.data.User;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.GrpcPost;
import fctreddit.impl.grpc.generated_java.UsersProtoBuf.GrpcUser;

public class DataModelAdaptor {

    public static User GrpcUser_to_User(GrpcUser from )  {
        return new User(
                from.getUserId(),
                from.getFullName(),
                from.getEmail(),
                from.getPassword(),
                from.getAvatarUrl());
    }

    public static GrpcUser User_to_GrpcUser(User from )  {
        GrpcUser.Builder b = GrpcUser.newBuilder()
                .setUserId( from.getUserId())
                .setPassword( from.getPassword())
                .setEmail( from.getEmail())
                .setFullName( from.getFullName());

        if(from.getAvatarUrl() != null)
            b.setAvatarUrl( from.getAvatarUrl());

        return b.build();
    }

    public static Post GrpcPost_to_Post(GrpcPost from) {
        return new Post (
                from.getPostId(),
                from.getAuthorId(),
                from.getCreationTimestamp(),
                from.getContent(),
                from.getMediaUrl(),
                from.getParentUrl(),
                from.getUpVote(),
                from.getDownVote()
        );
    }

    public static GrpcPost Post_to_GrpcPost(Post from) {
        GrpcPost.Builder b = GrpcPost.newBuilder()
                .setCreationTimestamp( from.getCreationTimestamp())
                .setUpVote( from.getUpVote())
                .setDownVote( from.getDownVote());

        if(from.getAuthorId() != null)
            b.setAuthorId( from.getAuthorId());
        if(from.getPostId() != null)
            b.setPostId( from.getPostId());
        if(from.getMediaUrl() != null)
            b.setMediaUrl( from.getMediaUrl());
        if(from.getParentUrl() != null)
            b.setParentUrl( from.getParentUrl());
        if(from.getContent() != null)
            b.setContent( from.getContent());

        return b.build();
    }
}
