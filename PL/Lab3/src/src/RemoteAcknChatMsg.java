import java.io.IOException;

public class RemoteAcknChatMsg extends LocalAcknChatMsg {

    private String acknUserId;
    private String senderUserId;

    public RemoteAcknChatMsg(String id, String acknUserId, String senderUserId) {
		super(id);
        this.acknUserId = acknUserId;
        this.senderUserId = senderUserId;
    }

	@Override
	public void execute(Peer peer) throws IOException {
        Chat.getInstance().receiveAckn(this, (Server) peer);
	}

    public String getAcknUserId() {
        return acknUserId;
    }

    public String getSenderUserId() {
        return senderUserId;
    }
}
