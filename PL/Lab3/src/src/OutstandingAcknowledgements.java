import java.util.LinkedList;

/*
 * Class to encapsulte all outstanding ACKs for a single Message to ensure correct ACKing
 * by the clients (no double ACKs...)
 */
public class OutstandingAcknowledgements {
    private String senderId;
    private LinkedList<String> recipientIds;
    private long creationTime;

    public OutstandingAcknowledgements(String senderId, long creationTime) {
        this.senderId = senderId;
        this.recipientIds = new LinkedList<>();
        this.creationTime = creationTime;
    }

    public String getSenderId() {
        return senderId;
    }

    public void add(String recipient) {
        recipientIds.add(recipient);
    }

    public void remove(String recipient) {
        recipientIds.remove(recipient);
    }

    public boolean contains(String recipient) {
        return recipientIds.contains(recipient);
    }

    public int size() {
        return recipientIds.size();
    }

    public long getAge() {
        return this.creationTime;
    }
}
