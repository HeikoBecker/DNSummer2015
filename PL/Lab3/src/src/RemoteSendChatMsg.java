import java.io.IOException;


public class RemoteSendChatMsg extends LocalSendChatMsg {
	String senderId;

	public RemoteSendChatMsg(String id, String recipient, String senderId,
			String message) {
		super(id, recipient, message);
		this.senderId = senderId;
	}

	@Override
	public void execute(Peer peer) throws IOException {
		Chat.getInstance().emitMessage(this, this.senderId);
	}
}
