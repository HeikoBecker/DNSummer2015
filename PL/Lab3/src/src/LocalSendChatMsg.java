import java.io.IOException;

/*
 * High Level encapsulation of a single SEND message.
 */
public class LocalSendChatMsg extends Message {
    private final String message;
    private final String recipient;

    public LocalSendChatMsg(String id, String recipient, String message) {
        this.type = "dnChat-SEND";
        this.id = id;
        this.recipient = recipient;
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public void execute(Peer peer) throws IOException {
        if (!peer.isAuthenticated()) {
            peer.emit(false, "INVD", "0");
            peer.exit();
        } else if (Chat.getInstance().isMessageIdTaken(this.id)) {
            // We only check for existing messageIds and not userIds, following the reference implementation.
            peer.emit(false, "FAIL", this.id, new String[]{"NUMBER"});
        } else if (this.message.length() > 384) {
            /*
             * Note: The length limitation to 384 bytes is derived by reversing the reference implementation.
             * https://dcms.cs.uni-saarland.de/dn/forum/viewtopic.php?f=3&t=124: Appropriate 300 is OK hence 
             * this should work
             */
            peer.emit(false, "FAIL", this.id, new String[]{"LENGTH"});
        } else {
            peer.emit(false, "OKAY", this.id);
            peer.recvSendChatMsg(this);
        }
    }
}
