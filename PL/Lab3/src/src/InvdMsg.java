import java.io.IOException;

public class InvdMsg extends Message {

    public InvdMsg() {
        this.Type = "dnChat-INVD";
    }

    @Override
    public void execute(Peer peer) throws IOException {
        peer.emit(false, "INVD", "0");
        peer.exit();
    }
}
