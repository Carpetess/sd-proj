package fctreddit.impl.server.grpc;

import fctreddit.api.data.Post;
import fctreddit.api.data.User;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf.GrpcPost;
import fctreddit.impl.grpc.generated_java.UsersProtoBuf.GrpcUser;

public class DataModelAdaptor {

    public static User GrpcUser_to_User(GrpcUser from )  {
        return new User(
                from.hasUserId() ? from.getUserId() : null,
                from.hasFullName() ? from.getFullName() : null,
                from.hasEmail()? from.getEmail() : null,
                from.hasPassword()? from.getPassword() : null,
                from.hasAvatarUrl() ? from.getAvatarUrl() : null);
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
                from.hasPostId() ? from.getPostId() : null,
                from.hasAuthorId() ? from.getAuthorId() : null,
                from.hasCreationTimestamp() ? from.getCreationTimestamp() : null,
                from.hasContent() ? from.getContent() : null,
                from.hasMediaUrl()? from.getMediaUrl() : null,
                from.hasParentUrl()? from.getParentUrl() : null,
                from.hasUpVote() ? from.getUpVote() : 0,
                from.hasDownVote() ? from.getDownVote() : 0
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
