import java.io.IOException;
import java.util.HashMap;

public class DNChat {
    private static DNChat instance = null;

    private HashMap<String, DNConnection> connections = new HashMap<>();

    private HashMap<String, String> messages = new HashMap<>();

    private DNChat() {
    }

    // TODO: this class has to be completely synchronized

    public static synchronized DNChat getInstance() {
        if (instance == null) {
            instance = new DNChat();
        }

        return instance;
    }

    public boolean isNameTaken(String userName) {
        boolean result = false;
        for (DNConnection existingConnection : connections.values()) {
            if (existingConnection.getUserName().equals(userName)) {
                result = true;
                break;
            }
        }
        return result;
    }

    public boolean isMessageIdTaken(String id) {
        return messages.containsKey(id);
    }

    public void sendMessage(SendMsg msg, DNConnection senderConnection) throws IOException {
        String recipient = msg.getRecipient();
        if (recipient.equals("*")) {
            for (DNConnection recipientConnection : connections.values()) {
                if (recipientConnection.getUserId() != senderConnection.getUserId()) {
                    recipientConnection.sendMessage(msg, senderConnection.getUserId());
                }
            }
        } else {
            connections.get(recipient).sendMessage(msg, senderConnection.getUserId());
        }
        // TODO: insert proper value
        messages.put(msg.getId(), "");
    }

    public void sendAcknowledgement(AcknMsg msg, DNConnection senderConnection) throws IOException {
        // TODO: ack should only be send to original recipients. This should be handeled somewhere.
        for (DNConnection recipientConnection : connections.values()) {
            if (recipientConnection.getUserId() != senderConnection.getUserId()) {
                recipientConnection.sendAckn(msg, senderConnection.getUserId());
            }
        }
    }

    public void addConnection(String id, DNConnection senderConnection) throws IOException {
        for (DNConnection existingConnection : connections.values()) {
            existingConnection.sendArrv(id, senderConnection.getUserName());
            senderConnection.sendArrv(existingConnection.getUserId(), existingConnection.getUserName());
        }
        connections.put(id, senderConnection);
    }

    public void closeConnection(String userId) throws IOException {
        connections.remove(userId);
        for (DNConnection recipientConnection : connections.values()) {
            recipientConnection.sendLeft(userId);
        }
    }
}