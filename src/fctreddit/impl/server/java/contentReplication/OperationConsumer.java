package fctreddit.impl.server.java.contentReplication;

import fctreddit.api.java.Content;
import fctreddit.impl.server.kafka.KafkaSubscriber;
import fctreddit.impl.server.kafka.RecordProcessor;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public class OperationConsumer {

    public OperationConsumer(KafkaSubscriber subscriber) {
        startSubscriber(subscriber);
    }


    private void startSubscriber(KafkaSubscriber subscriber) {
        subscriber.start(new RecordProcessor() {
            @Override
            public void onReceive(ConsumerRecord<String, String> r) {
                String operation = r.key();
                switch (operation) {
                    case Content.CREATE_POST -> ;
                    case Content.GET_POST -> ;
                    case Content.GET_POSTS -> ;
                    case Content.GET_POST_ANSWERS -> ;
                    case Content.UPDATE_POST -> ;
                    case Content.DELETE_POST -> ;
                    case Content.UPVOTE_POST -> ;
                    case Content.DOWNVOTE_POST -> ;
                    case Content.REMOVE_UPVOTE_POST -> ;
                    case Content.REMOVE_DOWNVOTE_POST -> ;
                    case Content.GET_UPVOTE -> ;
                    case Content.GET_DOWNVOTE -> ;
                    case Content.REMOVE_USER_TRACE -> ;
                }
            }
        });
    }
}
