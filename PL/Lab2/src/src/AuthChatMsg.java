import java.io.IOException;

/*
 * High Level encapsulation of a single AUTH message.
 * Statically checks against group password.
 */
public class AuthChatMsg extends Message {
    // We use our group password to authenticate users.
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
        } else if (Chat.getInstance().isUserIdTaken(this.id)) {
            // We only check for existing userIds and not messageIds, following the reference implementation.
            client.emit("FAIL", this.id, new String[]{"NUMBER"});
        } else if (Chat.getInstance().isNameTaken(name)) {
            client.emit("FAIL", this.id, new String[]{"NAME"});
        } else {
            client.emit("OKAY", this.id);
            client.authenticate(this.id, this.name);
        }
    }
}
