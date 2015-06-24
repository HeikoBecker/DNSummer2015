package dn.messages.chat;

import dn.Chat;
import dn.Client;
import dn.messages.Message;

import java.io.IOException;

public class AuthChatMsg extends Message {
    private static final String groupPassword = "3YnnafwB";

    private boolean validPassword = false;
    private String name;

    public AuthChatMsg(String id, String name, String password) {
        this.id = id;
        this.name = name;

        if (password.equals(groupPassword)) {
            this.validPassword = true;
        }
    }

    @Override
    public void execute(Client client) throws IOException {
        if (client.isAuthenticated()) {
            client.emit("INVD", "0");
            client.exit();
        } else if (!this.validPassword) {
            client.emit("FAIL", this.id, new String[]{"PASSWORD"});
        } else if (Chat.getInstance().isNameTaken(name)) {
            client.emit("FAIL", this.id, new String[]{"NAME"});
        } else {
            client.emit("OKAY", this.id);
            client.authenticate(this.id, this.name);
        }
    }
}
