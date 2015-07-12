import java.io.IOException;

/*
 * This class is only used when a remote server sends an ARRV message.
 * The ChatMsgCodec enforces this.
 * Therefore we can cast the peer to a server in the execute method.
 */
public class ArrvChatMsg extends Message {

    private String userName;
    private String description;
    private int hopCount;

    public ArrvChatMsg(String id, String userName, String description, int hopCount) {
        this.id = id;
        this.userName = userName;
        this.description = description;
        this.hopCount = hopCount;
    }

    @Override
    public void execute(Peer peer) throws IOException {
    	Server sender = (Server) peer;
        Chat.getInstance().receiveArrvBroadcast(this, sender);
        sender.registerClient(new RemoteClient(this.id, this.userName, this.hopCount));
    }

    public String getUserName() {
        return userName;
    }

    public int getHopCount() {
        return hopCount;
    }
}
