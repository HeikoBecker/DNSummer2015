import java.io.IOException;

/*
 * This class is only used when a remote server sends an ARRV message.
 * The ChatMsgCodec enforces this.
 * Therefore we can cast the peer to a server in the execute method.
 */
public class RemoteArrvChatMsg extends Message {

    private String userName;
    private String description;
    private int hopCount;

    public RemoteArrvChatMsg(String id, String userName, String description, int hopCount) {
        this.id = id;
        this.userName = userName;
        this.description = description;
        this.hopCount = hopCount;
    }

    @Override
    public void execute(Peer peer) throws IOException {
        Chat.getInstance().receiveArrv(this, (Server) peer);
    }

    public String getUserName() {
        return userName;
    }

    public int getHopCount() {
        return hopCount;
    }

    public String getDescription() {
        return description;
    }
}
