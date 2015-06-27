package dn.messages.chat;

import dn.Client;
import dn.messages.Message;

import java.io.IOException;

public class InvdMsg extends Message {
    @Override
    public void execute(Client client) throws IOException {
        client.emit("INVD", "0");
        client.exit();
    }
}
