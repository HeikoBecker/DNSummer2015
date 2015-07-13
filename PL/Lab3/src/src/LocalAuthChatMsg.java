import java.io.IOException;

/*
 * High Level encapsulation of a single AUTH message.
 * Statically checks against group password.
 */
public class LocalAuthChatMsg extends Message {
    // We use our group password to authenticate users.
    private static final String groupPassword = "3YnnafwB";

    private boolean validPassword = false;
    private String name;

    public LocalAuthChatMsg(String id, String name, String password) {
        this.Type = "dnChat-AUTH";
        this.id = id;
        this.name = name;

        if (password.equals(groupPassword)) {
            this.validPassword = true;
        }
    }

    @Override
    public void execute(Peer peer) throws IOException {
        if (peer.isAuthenticated()) {
            peer.emit(false, "INVD", "0");
            peer.exit();
        } else if (!this.validPassword) {
            peer.emit(false, "FAIL", this.id, new String[]{"PASSWORD"});
        } else if (Chat.getInstance().isUserIdTaken(this.id)) {
            // We only check for existing userIds and not messageIds, following the reference implementation.
            peer.emit(false, "FAIL", this.id, new String[]{"NUMBER"});
        } else if (Chat.getInstance().isNameTaken(name)) {
            peer.emit(false, "FAIL", this.id, new String[]{"NAME"});
        } else {
            peer.emit(false, "OKAY", this.id);
            peer.authenticate(this.id, this.name);
        }
    }
}
