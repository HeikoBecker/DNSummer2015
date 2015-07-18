import java.io.IOException;

public class InvdMsg extends Message {

    public InvdMsg() {
        this.type = "dnChat-INVD";
    }

    @Override
    public void execute(Peer peer) throws IOException {
        peer.emit(false, "INVD", "0");
        try {
			peer.exit();
		} catch (InternalServerException e) {
			System.out.println("Internal Server Error. Something went wrong with the internal typing.");
		}
    }
}
