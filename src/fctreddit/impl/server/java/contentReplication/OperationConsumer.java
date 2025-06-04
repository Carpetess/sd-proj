package fctreddit.impl.server.java.contentReplication;

import com.google.gson.Gson;
import fctreddit.api.data.Post;
import fctreddit.api.java.Content;
import fctreddit.api.java.Result;
import fctreddit.impl.server.kafka.KafkaSubscriber;
import fctreddit.impl.server.kafka.RecordProcessor;
import fctreddit.impl.server.kafka.SyncPoint;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.logging.Logger;

public class OperationConsumer {
    private static final Gson gson = new Gson();
    private static final Logger Log = Logger.getLogger(OperationConsumer.class.getName());

    private SyncPoint syncPoint;

    public OperationConsumer(KafkaSubscriber subscriber) {
        syncPoint = SyncPoint.getSyncPoint();
        startSubscriber(subscriber);
    }


    private void startSubscriber(KafkaSubscriber subscriber) {
        subscriber.start(new RecordProcessor() {
            @Override
            public void onReceive(ConsumerRecord<String, String> r) {
                Log.info("Received the following instructions: " + r.key() + " " + r.value());
                String operation = r.key();
                String[] params = r.value().split(":::");
                Content impl = new ContentReplicaProcessor();

                switch (operation) {
                    case Content.CREATE_POST -> createPost(params, impl, r.offset());
                    case Content.GET_POST_ANSWERS -> getPostAnswers(params, impl);
                    case Content.UPDATE_POST -> updatePost(params, impl, r.offset());
                    case Content.DELETE_POST -> deletePost(params, impl, r.offset());
                    case Content.UPVOTE_POST -> upvotePost(params, impl, r.offset());
                    case Content.DOWNVOTE_POST -> downvotePost(params, impl, r.offset());
                    case Content.REMOVE_UPVOTE_POST -> removeUpvotePost(params, impl, r.offset());
                    case Content.REMOVE_DOWNVOTE_POST -> removeDownvotePost(params, impl, r.offset());
                    case Content.REMOVE_USER_TRACE -> removeUserTrace(params, impl, r.offset());
                }
            }
        });
    }

    private void createPost(String[] parms, Content impl, long offset){
        Post post = gson.fromJson(parms[0], Post.class);
        String userPassword = parms[1];
        Result<String> res = impl.createPost(post, userPassword);
        Log.info("Created the following result: " + res.toString());
        syncPoint.setResult(offset, res);
    }
    private void getPostAnswers (String[] parms, Content impl){}
    private void updatePost(String[] parms, Content impl, long offset){
        String postId = parms[0];
        String userPassword = parms[1];
        Post post = gson.fromJson(parms[2], Post.class);
        Result<Post> res = impl.updatePost(postId, userPassword, post);
        syncPoint.setResult(offset, res);
    }
    private void deletePost(String[] parms, Content impl, long offset){
        String postId = parms[0];
        String userPassword = parms[1];
        Result<Void> res = impl.deletePost(postId, userPassword);
        syncPoint.setResult(offset, res);
    }
    private void upvotePost(String[] parms, Content impl, long offset){
        String postId = parms[0];
        String userId = parms[1];
        String userPassword = parms[2];
        Result<Void> res = impl.upVotePost(postId, userId, userPassword);
        syncPoint.setResult(offset, res);
    }
    private void downvotePost(String[] parms, Content impl, long offset){
        String postId = parms[0];
        String userId = parms[1];
        String userPassword = parms[2];
        Result<Void> res = impl.downVotePost(postId, userId, userPassword);
        syncPoint.setResult(offset, res);
    }
    private void removeUpvotePost(String[] parms, Content impl, long offset){
        String postId = parms[0];
        String userId = parms[1];
        String userPassword = parms[2];
        Result<Void> res = impl.removeUpVotePost(postId, userId, userPassword);
        syncPoint.setResult(offset, res);
    }
    private void removeDownvotePost(String[] parms, Content impl, long offset){
        String postId = parms[0];
        String userId = parms[1];
        String userPassword = parms[2];
        Result<Void> res = impl.removeDownVotePost(postId, userId, userPassword);
        syncPoint.setResult(offset, res);
    }
    private void removeUserTrace(String[] parms, Content impl, long offset){
        String userId = parms[0];
        String secret = parms[1];
        Result<Void> res = impl.removeUserTrace(userId, secret);
        syncPoint.setResult(offset, res);
    }

}
