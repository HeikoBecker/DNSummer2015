import java.io.IOException;

public class RemoteSrvrChatMsg extends Message {

    public RemoteSrvrChatMsg() {
        this.type = "dnChat-SRVR";
    }

    @Override
    public void execute(Peer peer) throws IOException {
        Chat.getInstance().advertiseCurrentUsers((Server) peer);
    }
}
