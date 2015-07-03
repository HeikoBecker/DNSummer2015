import java.io.IOException;

public class SrvrChatMsg extends Message {

    public SrvrChatMsg() {
        this.Type = "dnChat-SRVR";
    }

    @Override
    public void execute(Peer peer) throws IOException {

    }
}
