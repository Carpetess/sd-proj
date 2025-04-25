package fctreddit.api.java.resources;

import fctreddit.api.clients.clientFactories.ImageClientFactory;
import fctreddit.api.clients.clientFactories.UserClientFactory;
import fctreddit.api.data.Post;
import fctreddit.api.data.User;
import fctreddit.api.data.Vote;
import fctreddit.api.data.VoteId;
import fctreddit.api.java.Content;
import fctreddit.api.java.Result;
import fctreddit.api.java.util.Content.VoteType;
import fctreddit.api.server.persistence.Hibernate;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;


public class ContentJava implements Content {

    Logger Log = Logger.getLogger(ContentJava.class.getName());
    private final Hibernate hibernate;

    public ContentJava() {
        hibernate = Hibernate.getInstance();
    }

    @Override
    public Result<String> createPost(Post post, String userPassword) {
        post.setPostId(UUID.randomUUID().toString());
        post.setCreationTimestamp(System.currentTimeMillis());
        if (!isValid(post))
            return Result.error(Result.ErrorCode.BAD_REQUEST);


        Result<User> user = getUser(post.getAuthorId(), userPassword);
        if (!user.isOK())
            return Result.error(user.error());

        try {
            if (post.getParentUrl() != null) {
                String[] slice = post.getParentUrl().split("/");
                String postId = slice[slice.length - 1];
                if (hibernate.get(Post.class, postId) == null)
                    return Result.error(Result.ErrorCode.NOT_FOUND);
            }
            hibernate.persist(post);
        } catch (Exception e) {
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        return Result.ok(post.getPostId());
    }

    @Override
    public Result<List<String>> getPosts(long timestamp, String sortOrder) {
        if (sortOrder == null) {
            sortOrder = "";
        }
        String commentCountQuery = "(SELECT COUNT(r) FROM Post r WHERE r.parentUrl LIKE CONCAT('%', p.postId))";

        List<String> posts;
        try {
            switch (sortOrder) {
                case MOST_UP_VOTES ->
                        posts = hibernate.jpql("SELECT p.postId FROM Post p WHERE p.creationTimestamp >= " + timestamp + " AND p.parentUrl IS NULL ORDER BY p.upVote DESC, p.postId ASC", String.class);
                case MOST_REPLIES -> {
                    posts = hibernate.jpql("SELECT p.postId FROM Post p WHERE p.creationTimestamp >= " + timestamp + " AND p.parentUrl IS NULL ORDER BY " + commentCountQuery + " DESC,  p.postId ASC", String.class);
                }
                default ->
                        posts = hibernate.jpql("SELECT p.postId FROM Post p WHERE p.creationTimestamp >= " + timestamp + " AND p.parentUrl IS NULL ORDER BY p.creationTimestamp ASC", String.class);
            }
        } catch (Exception e) {
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        Log.info("Found " + posts.size() + " posts");
        return Result.ok(posts);
    }

    @Override
    public Result<Post> getPost(String postId) {
        Log.info("Getting post " + postId);
        if (postId == null)
            return Result.error(Result.ErrorCode.BAD_REQUEST);

        Post post;
        try {
            post = hibernate.get(Post.class, postId);
        } catch (Exception e) {
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        if (post == null)
            return Result.error(Result.ErrorCode.NOT_FOUND);
        return Result.ok(post);
    }

    @Override
    public Result<List<String>> getPostAnswers(String postId, long maxTimeout) {
        List<Post> posts;
        try {
            posts = hibernate.jpql("SELECT p FROM Post p WHERE p.parentUrl LIKE '%" + postId + "' ORDER BY p.creationTimestamp", Post.class);
        } catch (Exception e) {
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        return Result.ok(posts.stream().map(Post::getPostId).toList());
    }

    @Override
    public Result<Post> updatePost(String postId, String userPassword, Post post) {
        Post oldPost = null;

        try {
            oldPost = hibernate.get(Post.class, postId);

            if (oldPost == null){
                Log.severe("Post " + postId + " not found");
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }

            List<Post> comments= hibernate.jpql("SELECT p FROM Post p WHERE p.parentUrl LIKE '%" + postId + "'", Post.class);
            int commentCount = comments.size();
            if (!canEdit(post, oldPost, commentCount))
                return Result.error(Result.ErrorCode.BAD_REQUEST);
            update(post, oldPost);
            Result<User> res = getUser(oldPost.getAuthorId(), userPassword);
            if (!res.isOK())
                return Result.error(res.error());

            hibernate.update(oldPost);
        } catch (Exception e) {
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        return Result.ok(post);
    }

    @Override
    public Result<Void> deletePost(String postId, String userPassword) {
        try {
            UserClientFactory userClient;
            ImageClientFactory imageClient;
            userClient = UserClientFactory.getInstance();
            imageClient = ImageClientFactory.getInstance();
            Post post = hibernate.get(Post.class, postId);

            if (post == null)
                return Result.error(Result.ErrorCode.NOT_FOUND);

            Result<User> userRes = userClient.getUser(post.getAuthorId(), userPassword);
            if (!userRes.isOK())
                return Result.error(Result.ErrorCode.FORBIDDEN);
            List<Post> toDelete = deletePostHelper(postId);
            toDelete.add(post);
            Log.info("Deleting " + toDelete.size() + " posts");
            for (Post p : toDelete) {
                hibernate.delete(p);
            }

            if (post.getMediaUrl() != null)
                imageClient.deleteImage(post.getAuthorId(), parseImageId(post.getMediaUrl()), userPassword);

        } catch (Exception e) {
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }

        return Result.ok();
    }

    private List<Post> deletePostHelper(String postId) {
        List<Post> answers = hibernate.jpql("SELECT p FROM Post p WHERE p.parentUrl LIKE '%" + postId + "'", Post.class);
        List<Post> toDelete = new LinkedList<>(answers);
        for (Post answer : answers) {
            toDelete.addAll(deletePostHelper(answer.getPostId()));
        }
        return toDelete;
    }

    @Override
    public Result<Void> upVotePost(String postId, String userId, String userPassword) {
        try {
            Post post = hibernate.get(Post.class, postId);
            Result<Void> voteHelper = voteHelper(post, userId, userPassword, VoteType.UPVOTE);
            if (!voteHelper.isOK())
                return voteHelper;
            int upVotes = post.getUpVote();
            post.setUpVote(upVotes + 1);
            hibernate.update(post);
        } catch (Exception e) {
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        return Result.ok();
    }

    private Result<Void> voteHelper(Post post, String userId, String userPassword, VoteType voteType) {
        Result<User> res = getUser(userId, userPassword);
        if (!res.isOK())
            return Result.error(res.error());

        if (post == null)
            return Result.error(Result.ErrorCode.NOT_FOUND);

        Result<Vote> voteRes = getVote(post.getPostId(), userId);

        if (voteRes.isOK() && voteRes.value() != null && voteRes.value().getVoteType() != VoteType.NONE)
            return Result.error(Result.ErrorCode.CONFLICT);

        try {
            Vote vote = voteRes.value();
            if (vote == null)
                hibernate.persist(new Vote(post.getPostId(), userId, voteType));
            else {
                vote.setVoteType(voteType);
                hibernate.update(vote);
            }
        } catch (Exception e) {
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        return Result.ok();
    }

    private Result<Void> removeVoteHelper(Post post, String userId, String userPassword, VoteType voteType) {
        Result<User> res = getUser(userId, userPassword);
        if (!res.isOK())
            return Result.error(res.error());
        if (post == null)
            return Result.error(Result.ErrorCode.NOT_FOUND);
        Result<Vote> voteRes = getVote(post.getPostId(), userId);

        if (voteRes.isOK() && (voteRes.value() == null || voteRes.value().getVoteType() != voteType))
            return Result.error(Result.ErrorCode.CONFLICT);

        try {
            Vote vote = voteRes.value();
            vote.setVoteType(VoteType.NONE);
            hibernate.update(vote);
        } catch (Exception e) {
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }

        return Result.ok();
    }

    @Override
    public Result<Void> removeUpVotePost(String postId, String userId, String userPassword) {
        try {
            Post post = hibernate.get(Post.class, postId);
            Result<Void> voteHelper = removeVoteHelper(post, userId, userPassword, VoteType.UPVOTE);
            if (!voteHelper.isOK())
                return voteHelper;
            int upVotes = post.getUpVote();
            post.setUpVote(upVotes - 1);
            hibernate.update(post);
        } catch (Exception e) {
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        return Result.ok();
    }

    @Override
    public Result<Void> downVotePost(String postId, String userId, String userPassword) {
        try {
            Post post = hibernate.get(Post.class, postId);
            Result<Void> voteHelper = voteHelper(post, userId, userPassword, VoteType.DOWNVOTE);
            if (!voteHelper.isOK())
                return voteHelper;
            int downVotes = post.getDownVote();
            post.setDownVote(downVotes + 1);
            hibernate.update(post);
        } catch (Exception e) {
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        return Result.ok();
    }

    @Override
    public Result<Void> removeDownVotePost(String postId, String userId, String userPassword) {
        try {
            Post post = hibernate.get(Post.class, postId);
            Result<Void> voteHelper = removeVoteHelper(post, userId, userPassword, VoteType.DOWNVOTE);
            if (!voteHelper.isOK())
                return voteHelper;
            int downVotes = post.getDownVote();
            post.setDownVote(downVotes - 1);
            hibernate.update(post);
        } catch (Exception e) {
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        return Result.ok();
    }

    @Override
    public Result<Integer> getupVotes(String postId) {
        Post post;
        try {
            post = hibernate.get(Post.class, postId);
        } catch (Exception e) {
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        if (post == null)
            return Result.error(Result.ErrorCode.NOT_FOUND);

        return Result.ok(post.getUpVote());
    }

    @Override
    public Result<Integer> getDownVotes(String postId) {
        Post post;
        try {
            post = hibernate.get(Post.class, postId);
        } catch (Exception e) {
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        if (post == null)
            return Result.error(Result.ErrorCode.NOT_FOUND);
        return Result.ok(post.getDownVote());
    }


    @Override
    public Result<Void> updatePostOwner(String userId, String password) {
        Result<User> res = getUser(userId, password);
        if (!res.isOK())
            return Result.error(res.error());

        List<Post> posts;
        try {
            posts = hibernate.jpql("SELECT p FROM Post p WHERE p.authorId = '" + userId + "'", Post.class);
            hibernate.update(posts);
        } catch (Exception e) {
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        return Result.ok();
    }

    @Override
    public Result<Void> removeAllUserVotes(String userId, String password) {
        Result<User> res = getUser(userId, password);
        if (!res.isOK())
            return Result.error(res.error());
        try {
            List<Vote> votes = hibernate.jpql("SELECT v FROM Vote v WHERE v.voterId = '" + userId + "'", Vote.class);
            for (Vote vote : votes) {
                hibernate.delete(vote);
            }
        } catch (Exception e) {
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        return Result.ok();
    }

    private Result<User> getUser(String userId, String password) {
        UserClientFactory userClient;
        try {
            userClient = UserClientFactory.getInstance();
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        return userClient.getUser(userId, password);
    }


    private Result<Vote> getVote(String postId, String userId) {
        try {
            VoteId voteId = new VoteId(postId, userId);
            Vote vote = hibernate.get(Vote.class, voteId);
            return Result.ok(vote);
        } catch (Exception e) {
            Log.severe(e.toString());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    private boolean isValid(Post post) {
        return post.getContent() != null && !post.getContent().isEmpty()
                && post.getAuthorId() != null && !post.getAuthorId().isBlank();
    }

    private boolean canEdit(Post post, Post oldPost, int commentCount) {
        return (post.getAuthorId() == null || post.getAuthorId().isBlank())
                && commentCount == 0;
    }
    private void update (Post newPost, Post oldPost) {
        if (newPost.getContent() != null )
            oldPost.setContent(newPost.getContent());
        if (newPost.getMediaUrl() != null)
            oldPost.setMediaUrl(newPost.getMediaUrl());
    }
    private String parseImageId(String url) {
        String[] slice = url.split("/");
        String imageId = slice[slice.length - 1];
        return imageId;
    }

}
