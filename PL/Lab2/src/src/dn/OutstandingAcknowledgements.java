package dn;

import java.util.LinkedList;

/*
 * Class to encapsulte all outstanding ACKs for a single Message to ensure correct ACKing
 * by the clients (no double ACKs...)
 */
public class OutstandingAcknowledgements {
    private String senderId;
    private LinkedList<String> recipientIds;


    public OutstandingAcknowledgements(String senderId) {
        this.senderId = senderId;
        this.recipientIds = new LinkedList<>();
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
}
