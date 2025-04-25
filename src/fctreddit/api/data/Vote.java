package fctreddit.api.data;

import fctreddit.api.java.util.Content.VoteType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

@Entity
@IdClass(VoteId.class)
public class Vote {

    @Id
    private String voterId;
    @Id
    private String postId;
    private VoteType voteType;

    public Vote(String voterId, String postId, VoteType voteType) {
        this.voterId = voterId;
        this.postId = postId;
        this.voteType = voteType;
    }

    public Vote() {
    }



    public String getPostId() { return postId; }
    public VoteType getVoteType() { return voteType; }
    public String getVoterId() { return voterId; }

    public void setPostId(String postId) { this.postId = postId; }
    public void setVoteType(VoteType voteType) { this.voteType = voteType; }
    public void setVoterId(String voterId) { this.voterId = voterId; }
}
