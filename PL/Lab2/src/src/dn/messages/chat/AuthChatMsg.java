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
    public void execute(Client connection) throws IOException {
        if (connection.isAuthenticated()) {
            connection.send("INVD", "0");
            connection.close();
        } else if (!this.validPassword) {
            connection.send("FAIL", this.id, new String[]{"PASSWORD"});
        } else if (Chat.getInstance().isNameTaken(name)) {
            connection.send("FAIL", this.id, new String[]{"NAME"});
        } else {
            connection.send("OKAY", this.id);
            connection.authenticate(this.id, this.name);
        }
    }
}
