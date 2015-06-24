package dn;

import dn.messages.chat.AcknChatMsg;
import dn.messages.chat.SendChatMsg;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

public class Chat {
    private static Chat instance = null;

    private HashMap<String, Client> clients = new HashMap<>();

    private HashMap<String, String> messages = new HashMap<>();

    private Chat() {
    }

    // TODO: this class has to be completely synchronized

    public static synchronized Chat getInstance() {
        if (instance == null) {
            instance = new Chat();
        }

        return instance;
    }

    public boolean isNameTaken(String userName) {
        boolean result = false;
        for (Client existingClient : clients.values()) {
            if (existingClient.getUserName().equals(userName)) {
                result = true;
                break;
            }
        }
        return result;
    }

    public boolean isMessageIdTaken(String id) {
        return messages.containsKey(id);
    }

    public void sendMessage(SendChatMsg msg, Client sendingClient) throws IOException {
        String recipientName = msg.getRecipient();
        if (recipientName.equals("*")) {
            for (Client receivingClient : clients.values()) {
                if (!receivingClient.equals(sendingClient)) {
                    receivingClient.emitSendChatMsg(msg, sendingClient.getUserId());
                }
            }
        } else {
            clients.get(recipientName).emitSendChatMsg(msg, sendingClient.getUserId());
        }
        // TODO: insert proper value
        messages.put(msg.getId(), "");
    }

    public void sendAcknowledgement(AcknChatMsg msg, Client sendingClient) throws IOException {
        // TODO: ack should only be send to original recipients. This should be handeled somewhere.
        for (Client receivingClient : clients.values()) {
            if (!Objects.equals(receivingClient.getUserId(), sendingClient.getUserId())) {
                receivingClient.emitAcknChatMsg(msg, sendingClient.getUserId());
            }
        }
    }

    public void registerClient(Client newClient) throws IOException {
        for (Client existingClient : clients.values()) {
            existingClient.emitArrvChatMsg(newClient);
            newClient.emitArrvChatMsg(existingClient);
        }
        clients.put(newClient.getUserId(), newClient);
    }

    public void unregisterClient(String userId) throws IOException {
        clients.remove(userId);
        for (Client recipientConnection : clients.values()) {
            recipientConnection.sendLeft(userId);
        }
    }
}