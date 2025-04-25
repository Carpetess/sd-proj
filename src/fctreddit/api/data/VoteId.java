package fctreddit.api.data;

import java.io.Serializable;
import java.util.Objects;

//Done by chatGPT
public class VoteId implements Serializable {
    private String voterId;
    private String postId;

    public VoteId() {}

    public VoteId(String voterId, String postId) {
        this.voterId = voterId;
        this.postId = postId;
    }

    // hashCode & equals (important!)
    @Override
    public int hashCode() {
        return Objects.hash(voterId, postId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof VoteId)) return false;
        VoteId that = (VoteId) obj;
        return Objects.equals(voterId, that.voterId) &&
                Objects.equals(postId, that.postId);
    }

    // getters & setters (optional but good)
}
