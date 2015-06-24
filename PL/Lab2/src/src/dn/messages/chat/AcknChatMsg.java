package dn.messages.chat;

import dn.Chat;
import dn.Client;
import dn.messages.Message;

import java.io.IOException;

public class AcknChatMsg extends Message {

    public AcknChatMsg(String id) {
        this.id = id;
    }

    @Override
    public void execute(Client connection) throws IOException {
        if (!connection.isAuthenticated()) {
            connection.send("INVD", "0");
            connection.close();
        } else if (!Chat.getInstance().isMessageIdTaken(this.id)) {
            connection.send("FAIL", this.id, new String[]{"NUMBER"});
        } else {
            connection.recvAckn(this);
        }
    }
}
