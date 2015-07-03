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
        this.Type = "dnChat-AUTH";
        this.Id = id;
        this.name = name;

        if (password.equals(groupPassword)) {
            this.validPassword = true;
        }
    }

    @Override
    public void execute(Peer peer) throws IOException {
        if (peer.isAuthenticated()) {
            peer.emit("INVD", "0");
            peer.exit();
        } else if (!this.validPassword) {
            peer.emit("FAIL", this.Id, new String[]{"PASSWORD"});
        } else if (Chat.getInstance().isUserIdTaken(this.Id)) {
            // We only check for existing userIds and not messageIds, following the reference implementation.
            peer.emit("FAIL", this.Id, new String[]{"NUMBER"});
        } else if (Chat.getInstance().isNameTaken(name)) {
            peer.emit("FAIL", this.Id, new String[]{"NAME"});
        } else {
            peer.emit("OKAY", this.Id);
            peer.authenticate(this.Id, this.name);
        }
    }
}
