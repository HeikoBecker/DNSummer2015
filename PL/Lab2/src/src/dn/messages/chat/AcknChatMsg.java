package dn.messages.chat;

import dn.Chat;
import dn.Client;
import dn.messages.Message;

import java.io.IOException;

/*
 *  High Level encapsulation of a single ACKN message.
 */
public class AcknChatMsg extends Message {

    public AcknChatMsg(String id) {
        this.id = id;
    }

    @Override
    public void execute(Client client) throws IOException {
        if (!client.isAuthenticated()) {
            client.emit("INVD", "0");
            client.exit();
        } else if (!Chat.getInstance().isMessageIdOpenForAckn(this.id, client)) { //TODO: Duplicate ACKS?
            client.emit("FAIL", this.id, new String[]{"NUMBER"});
        } else {
            client.recvAcknChatMsg(this);
        }
    }
}
