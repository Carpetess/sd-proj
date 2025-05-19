package fctreddit.api.data;

import fctreddit.api.java.util.Content.VoteType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

@Entity
public class Vote {
    @Id
    private String voterId;
    @Id
    private String postId;
    private boolean upVote;

    public Vote(String voterId, String postId, boolean upVote) {
        this.voterId = voterId;
        this.postId = postId;
        this.upVote = upVote;
    }

    public Vote() {
    }

    public String getPostId() { return postId; }
    public boolean isUpVote() { return upVote; }
    public String getVoterId() { return voterId; }

    public void setPostId(String postId) { this.postId = postId; }
    public void setVoteType(boolean upVote) { this.upVote = upVote; }
    public void setVoterId(String voterId) { this.voterId = voterId; }
}
