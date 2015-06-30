import java.io.IOException;

/*
 * High Level encapsulation of a single SEND message.
 */
public class SendChatMsg extends Message {
    private final String message;
    private final String recipient;

    public SendChatMsg(String id, String recipient, String message) {
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
    public void execute(Client client) throws IOException {
        if (!client.isAuthenticated()) {
            client.emit("INVD", "0");
            client.exit();
        } else if (Chat.getInstance().isMessageIdTaken(this.id)) { //TODO: Same as for AuthChatMsg.
            client.emit("FAIL", this.id, new String[]{"NUMBER"});
        } else if (this.message.length() > 384) {
            /*
             * Note: The length limitation to 384 bytes is derived by reversing the reference implementation.
             * https://dcms.cs.uni-saarland.de/dn/forum/viewtopic.php?f=3&t=124: Appropriate 300 is OK hence 
             * this should work
             */
            client.emit("FAIL", this.id, new String[]{"LENGTH"});
        } else {
            client.emit("OKAY", this.id);
            client.recvSendChatMsg(this);
        }
    }
}
