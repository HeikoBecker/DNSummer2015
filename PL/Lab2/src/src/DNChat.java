import java.io.IOException;
import java.util.HashMap;

public class DNChat {
    private static DNChat instance = null;

    private HashMap<String, DNConnection> connections = new HashMap<>();

    private DNChat() {
    }

    public static synchronized DNChat getInstance() {
        if (instance == null) {
            instance = new DNChat();
        }

        return instance;
    }

    public void addConnection(String id, DNConnection connection) {
        connections.put(id, connection);
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
    }


}