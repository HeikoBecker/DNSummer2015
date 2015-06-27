package dn;

import dn.messages.chat.AcknChatMsg;
import dn.messages.chat.SendChatMsg;

import java.io.IOException;
import java.util.HashMap;

/*
 * Global Chat Server instance.
 * Aggregates information: Authenticated clients with ID, Messages and outstanding ACKs
 */
public class Chat {
    private static Chat instance = null;

    // Client ID -> Client
    private HashMap<String, Client> clients = new HashMap<>();

    // Message ID -> Collection of outstanding acknowledgements
    private HashMap<String, OutstandingAcknowledgements> outstandingAcks = new HashMap<>();

    private Chat() {
    }

    public static synchronized Chat getInstance() {
        if (instance == null) {
            instance = new Chat();
        }

        return instance;
    }

    // ----------------- COLLISION CHECKS -----------------

    public synchronized boolean isNameTaken(String userName) {
        boolean result = false;
        for (Client existingClient : clients.values()) {
            if (existingClient.getUserName().equals(userName)) {
                result = true;
                break;
            }
        }
        return result;
    }

    public synchronized boolean isUserIdTaken(String userId) {
        return clients.containsKey(userId);
    }

    public synchronized boolean isMessageIdTaken(String messageId) {
        return outstandingAcks.containsKey(messageId);
    }

    public synchronized boolean isMessageIdOpenForAckn(String messageId, Client otherClient) {
        return outstandingAcks.containsKey(messageId) && outstandingAcks.get(messageId).contains(otherClient.getUserId());
    }

    // ----------------- RELAYING MESSAGES TO OTHER CLIENTS -----------------

    public synchronized void emitMessage(SendChatMsg msg, Client sendingClient) throws IOException {
        String recipientName = msg.getRecipient();
        if (recipientName.equals("*")) {
            for (Client receivingClient : clients.values()) {
                if (!receivingClient.equals(sendingClient)) {
                    receivingClient.emitSendChatMsg(msg, sendingClient.getUserId());
                    storeMessage(msg.getId(), sendingClient.getUserId(), receivingClient.getUserId());
                }
            }
        } else {
            Client receivingClient = clients.get(recipientName);
            receivingClient.emitSendChatMsg(msg, sendingClient.getUserId());
            storeMessage(msg.getId(), sendingClient.getUserId(), receivingClient.getUserId());
        }
    }

    public synchronized void emitAcknowledgement(AcknChatMsg msg, Client sendingClient) throws IOException {
        if (outstandingAcks.containsKey(msg.id)) {
            String receiverId = sendingClient.getUserId();
            clients.get(outstandingAcks.get(msg.id).getSenderId()).emitAcknChatMsg(msg, receiverId);
            removeMessage(msg.id, receiverId);
        }
    }

    /*
     * A message is stored so future acknowledgements can be checked.
     */
    private void storeMessage(String messageId, String senderId, String receiverId) {
        if (!outstandingAcks.containsKey(messageId)) {
            outstandingAcks.put(messageId, new OutstandingAcknowledgements(senderId));
        }
        // TODO: start a timer and remove message when timer expired
        outstandingAcks.get(messageId).add(receiverId);
    }

    /*
     * Removes outstanding acknowledgement. We comply to the reference implementation, as mentioned in:
     * https://dcms.cs.uni-saarland.de/dn/forum/viewtopic.php?f=3&t=109
     *
     * A message's id can be reused, when all clients have sent their acknowledgement.
     *
     */
    private void removeMessage(String messageId, String receiverId) {
        outstandingAcks.get(messageId).remove(receiverId);
        if (outstandingAcks.get(messageId).size() == 0) {
            outstandingAcks.remove(messageId);
        }
    }


    // ----------------- ENTER / LEAVE -----------------

    /*
     * Add a client and tell all other client that she joined.
     * Inform the newly joined client about others in the room.
     */
    public synchronized void registerClient(Client newClient) throws IOException {
        for (Client existingClient : clients.values()) {
            existingClient.emitArrvChatMsg(newClient);
            newClient.emitArrvChatMsg(existingClient);
        }
        clients.put(newClient.getUserId(), newClient);
    }

    /*
     * Remove the client and tell all other clients that she left.
     */
    public synchronized void unregisterClient(String userId) throws IOException {
        clients.remove(userId);
        for (Client existingClient : clients.values()) {
            existingClient.emitLeftChatMsg(userId);
        }
    }
}