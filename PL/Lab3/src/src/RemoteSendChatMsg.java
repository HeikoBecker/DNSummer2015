import java.io.IOException;


public class RemoteSendChatMsg extends SendChatMsg {

	String id,recipient,senderId,message;
	
	public RemoteSendChatMsg(String id, String recipient, String senderId,
			String message) {
		super(id, recipient, message);
		this.senderId = senderId;
	}

	@Override
	public void execute(Peer peer) throws IOException {
		System.out.println("Received a remote message ");
		//TODO: Make the method below accept any peer in Chat class
		//Then we can reuse it also for broadcasting here
		//Chat.getInstance().emitMessage(this, peer);
	}

}
