import java.io.IOException;

public class AcknMsg extends Message {

    public AcknMsg(String id) {
        this.id = id;
    }

    @Override
    public void execute(DNConnection connection) throws IOException {
        if (!connection.isAuthenticated()) {
            connection.send("INVD", "0");
            connection.close();
        } else if (!DNChat.getInstance().isMessageIdTaken(this.id)) {
            connection.send("FAIL", this.id, new String[]{"NUMBER"});
        } else {
            connection.recvAckn(this);
        }
    }
}
