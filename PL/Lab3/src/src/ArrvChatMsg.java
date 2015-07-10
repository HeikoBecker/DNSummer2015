import java.io.IOException;

public class ArrvChatMsg extends Message {

    private String userName;
    private String description;
    private int hopCount;

    public ArrvChatMsg(String id, String userName, String description, int hopCount) {
        this.Id = id;
        this.userName = userName;
        this.description = description;
        this.hopCount = hopCount;
    }

    @Override
    public void execute(Peer peer) throws IOException {
        Chat.getInstance().receiveArrvBroadcast(this, peer);
    }

    public String getUserName() {
        return userName;
    }

    public int getHopCount() {
        return hopCount;
    }
}
