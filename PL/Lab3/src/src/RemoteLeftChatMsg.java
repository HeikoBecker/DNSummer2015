import java.io.IOException;

public class RemoteLeftChatMsg extends Message {
    public RemoteLeftChatMsg(String id) {
        this.id = id;
    }

    @Override
    public void execute(Peer peer) throws IOException {
        Chat.getInstance().receiveLeft(this, (Server) peer);
    }
}
