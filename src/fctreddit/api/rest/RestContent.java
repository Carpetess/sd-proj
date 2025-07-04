package fctreddit.api.rest;

import java.util.List;

import fctreddit.api.data.Post;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path(RestContent.PATH)
public interface RestContent {

	public static final String PATH = "/posts";
	public static final String PASSWORD = "pwd";
	public static final String POSTID = "postId";
	public static final String TIMESTAMP = "timestamp";
	public static final String REPLIES = "replies";
	public static final String UPVOTE = "upvote";
	public static final String DOWNVOTE = "downvote";
	public static final String USERID = "userId";
	public static final String SORTBY = "sortBy";
	public static final String TIMEOUT = "timeout";
	public static final String SECRET = "secret";
	/**
	 * The following constants are the values that can be sent for the query parameter SORTBY
	 **/
	public static final String MOST_UP_VOTES = "votes";
	public static final String MOST_REPLIES = "replies";
	
	
	/**
	 * Creates a new Post (that can be an answer to another Post, in which case the parentURL should be
	 * a valid URL for another post), generating its unique identifier. 
	 * The result should be the identifier of the Post in case of success.
	 * The creation timestamp of the post should be set to be the time in the server when the request
	 * was received.
	 * 
	 * @param post - The Post to be created, that should contain the userId of the author in the appropriate field.
	 * @param userPassword - the password of author of the new post
	 * @return OK and PostID if the post was created;
	 * NOT FOUND, if the owner of the post does not exist, or the parentPost (if not null) does not exists;
	 * FORBIDDEN, if the password is not correct;
	 * BAD_REQUEST, otherwise.
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String createPost(Post post, @QueryParam(PASSWORD) String userPassword);
	
	/**
	 * Retrieves a list with all top-level Posts unique identifiers (i.e., Posts that have no parent Post).
	 * By default (i.e., when no query parameter is passed) all top-level posts should be returned in the 
	 * order in which they were created. The effects of both optional parameters can be combined to affect
	 * the answer.
	 * 
	 * @param timestamp this is an optional parameter, if it is defined then the returned list
	 * should only contain Posts whose creation timestamp is equal or above the provided timestamp.
	 * @param sortOrder this is an optional parameter, the admissible values are on constants MOST_UP_VOTES
	 * and MOST_REPLIES, if the first is indicated, posts IDs should be ordered from the Post with more votes
	 * to the one with less votes. If the second is provided posts IDs should be ordered from the Post with 
	 * more direct replies to the one with less direct replies.
	 * if there are posts with the same number of up votes or direct replies, respectively, those should be
	 * ordered by the lexicographic order of the PostID.
	 * @return 	OK and the List of PostIds that match all options in the right order 
	 * 			
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> getPosts(@QueryParam(TIMESTAMP) long timestamp, @QueryParam(SORTBY) String sortOrder);
	
	/**
	 * Retrieves a given post.
	 * 
	 * @param postId the unique identifier of the short to be retrieved
	 * @return 	OK and the Post in case of success 
	 * 			NOT_FOUND if postId does not match an existing Post
	 */
	@GET
	@Path("{" + POSTID + "}")
	@Produces(MediaType.APPLICATION_JSON)
	public Post getPost(@PathParam(POSTID) String postId);
	
	/**
	 * Retrieves a list with all unique identifiers of posts that have the post
	 * identified by the postId as their parent (i.e., the replies to that post),
	 * the order should be the creation order of those posts.
	 * @param postId the postId for which answers want to be obtained
	 * @param timeout (optional) indicates the maximum amount of time that this operation should
	 * 		  wait (before returning a reply to the client) for a new answer to be added
	 * 		  to the post. If a new answer is added to the target post after the start of 
	 * 		  the execution of this operation and before the timeout expires an answer should
	 * 		  be sent to the client at that time. 		   
	 * @return 	OK and the List of PostIds that are answers to the post ordered by creationTime 
	 * 			NOT_FOUND if postId does not match an existing Post			
	 */
	@GET
	@Path("{" + POSTID + "}/" + REPLIES)
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> getPostAnswers(@PathParam(POSTID) String postId, @QueryParam(TIMEOUT) long timeout);
	
	/**
	 * Updates the contents of a post restricted to the fields:
	 * - content
	 * - mediaUrl
	 * @param postId the post that should be updated
	 * @param userPassword the password, it is assumed that only the author of the post 
	 * can updated it, and as such, the password sent in the operation should belong to 
	 * that user.
	 * @param post A post object with the fields to be updated
	 * @return 	OK the updated post, in case of success.
	 * 			FORBIDDEN, if the password is not correct;
	 * 			BAD_REQUEST, otherwise.
	 */
	@PUT
	@Path("{" + POSTID + "}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Post updatePost(@PathParam(POSTID) String postId, @QueryParam(PASSWORD) String userPassword, Post post);
	
	/**
	 * Deletes a given Post, only the author of the Post can do this operation. A successful delete will also remove
	 * any reply to this post (or replies to those replies) even if performed by different authors, however, images
	 * associated to replies (and replies to replies) should not be deleted by the effects of this operations.
	 * 
	 * @param postId the unique identifier of the Post to be deleted
	 * @return 	NO_CONTENT in case of success 
	 * 			NOT_FOUND if postId does not match an existing post
	 * 			FORBIDDEN if the password is not correct (it should always be considered the authorId 
	 * 					  of the post as the user that is attempting to execute this operation);
	 */	
	@DELETE
	@Path("{" + POSTID + "}")
	public void deletePost(@PathParam(POSTID) String postId, @QueryParam(PASSWORD) String userPassword);
	
	/**
	 * Adds an upvote to a given post made by a specific user (might be different from the author
	 * of the post). The vote must be authenticated by the password of the user adding the upvote. 
	 * The upvote on a post can be only be made once by an user, and the user must not have a downvote
	 * on that post.
	 * @param postId unique identifier of the post over which the upvote is made
	 * @param userId unique identifier of the user making the upvote
	 * @param userPassword Password of user making the upvote
	 * @return 	NO_CONTENT in case of success
	 * 			NOT_FOUND if the postId does not match an existing post or the user does not exists
	 * 			FORBIDDEN if the password is not correct or the user doing the operation is not the owner
	 * 			CONFLICT if the user already has made an upvote or downvote on the post
	 *			BAD_REQUEST otherwise
	 */
	@POST
	@Path("{" + POSTID + "}/" + UPVOTE + "/{" + USERID + "}" )
	public void upVotePost(@PathParam(POSTID) String postId, @PathParam(USERID) String userId, @QueryParam(PASSWORD) String userPassword);
	
	/**
	 * Removes a previously added upvote to a given post made by a specific user (might be different from the author
	 * of the post). The action must be authenticated by the password of the user removing the upvote.
	 * @param postId unique identifier of the post over which the upvote is removed
	 * @param userId unique identifier of the user removing the upvote
	 * @param userPassword Password of user removing the upvote
	 * @return 	NO_CONTENT in case of success
	 * 			NOT_FOUND if the postId does not match an existing post or the user does not exists
	 * 			FORBIDDEN if the password is not correct
	 * 			CONFLICT if the user had not made an upvote on this post previously
	 *			BAD_REQUEST otherwise
	 */
	@DELETE
	@Path("{" + POSTID + "}/" + UPVOTE + "/{" + USERID + "}" )
	public void removeUpVotePost(@PathParam(POSTID) String postId, @PathParam(USERID) String userId, @QueryParam(PASSWORD) String userPassword);
	
	/**
	 * Adds an downvote to a given post made by a specific user (might be different from the author
	 * of the post). The vote must be authenticated by the password of the user adding the downvote. 
	 * The downvote on a post can be only be made once by an user, and the user must not have a upvote
	 * on that post.
	 * @param postId unique identifier of the post over which the downvote is made
	 * @param userId unique identifier of the user making the downvote
	 * @param userPassword Password of user making the downvote
	 * @return 	NO_CONTENT in case of success
	 * 			NOT_FOUND if the postId does not match an existing post or the user does not exists
	 * 			FORBIDDEN if the password is not correct
	 * 			CONFLICT if the user already has made an upvote or downvote on the post
	 *			BAD_REQUEST otherwise
	 */
	@POST
	@Path("{" + POSTID + "}/" + DOWNVOTE + "/{" + USERID + "}" )
	public void downVotePost(@PathParam(POSTID) String postId, @PathParam(USERID) String userId, @QueryParam(PASSWORD) String userPassword);
	
	/**
	 * Removes a previously added downvote to a given post made by a specific user (might be different from the author
	 * of the post). The action must be authenticated by the password of the user removing the downvote. 
	 * @param postId unique identifier of the post over which the downvote is removed
	 * @param userId unique identifier of the user removing the downvote
	 * @param userPassword Password of user removing the downvote
	 * @return 	NO_CONTENT in case of success
	 * 			NOT_FOUND if the postId does not match an existing post or the user does not exists
	 * 			FORBIDDEN if the password is not correct
	 * 			CONFLICT if the user had not made an downvote on this post previously
	 *			BAD_REQUEST otherwise
	 */
	@DELETE
	@Path("{" + POSTID + "}/" + DOWNVOTE + "/{" + USERID + "}" )
	public void removeDownVotePost(@PathParam(POSTID) String postId, @PathParam(USERID) String userId, @QueryParam(PASSWORD) String userPassword);
	
	/**
	 * Exposes the number of upvotes currently associated with a given post
	 * @param postId the post that is targeted by this operation
	 * @return	OK and the number of upvotes in case of success
	 * 			NOT_FOUND if the postId does not match an existing post
	 */
	@GET
	@Path("{" + POSTID + "}/" + UPVOTE)
	@Consumes(MediaType.APPLICATION_JSON)
	public Integer getupVotes(@PathParam(POSTID) String postId);
	
	/**
	 * Exposes the number of downvotes currently associated with a given post
	 * @param postId the post that is targeted by this operation
	 * @return	OK and the number of downvotes in case of success
	 * 			NOT_FOUND if the postId does not match an existing post
	 */
	@GET
	@Path("{" + POSTID + "}/" + DOWNVOTE)
	@Consumes(MediaType.APPLICATION_JSON)
	public Integer getDownVotes(@PathParam(POSTID) String postId);

	/**
	 * Sets the authorId of the posts owned by this user to null.
	 * @param userId author of all the posts to be updated.
	 * @param secret auth between servers
	 * @return NO_CONTENT if all the posts that user used to own were set to null, FORBIDDEN if the password didn't match,
	 * NOT_FOUND if the author didn't exist.
	 */
	@DELETE
	@Path("{" + USERID + "}/removeTraces")
	@Consumes(MediaType.APPLICATION_JSON)
	public void removeUserTraces(@PathParam(USERID) String userId, @QueryParam(SECRET) String secret);

}
