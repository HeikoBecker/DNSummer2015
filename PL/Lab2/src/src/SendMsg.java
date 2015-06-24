import java.io.IOException;

public class SendMsg extends Message {
    private final String message;
    private final String recipient;

    public SendMsg(String id, String recipient, String message) {
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
    public void execute(DNConnection connection) throws IOException {
        if (!connection.isAuthenticated()) {
            connection.send("INVD", "0");
            connection.close();
        } else if (this.message.length() > 384) {
            /*
             * Note: The length limitation to 384 bytes is derived by reversing the reference implementation.
             */
            connection.send("FAIL", this.id, new String[]{"LENGTH"});
        } else if (DNChat.getInstance().isMessageIdTaken(this.id)) {
            connection.send("FAIL", this.id, new String[]{"NUMBER"});
        } else {
            connection.send("OKAY", this.id);
            connection.recvMsg(this);
        }
    }
}
