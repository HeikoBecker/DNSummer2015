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
    	Chat chat = Chat.getInstance();
    	if (!chat.broadcasted(this.id)){
        	Server sender = (Server) peer;
    		chat.receiveArrvBroadcast(this, sender);
    		// the RemoteClient should be added to the sending Server no matter how far away it is
    		// otherwise we are not able to tell the presence of this user to local clients anymore
            sender.registerClient(new RemoteClient(this.id, this.userName, this.hopCount));
    	}else{
    		//TODO: Maybe log, that there was a message received that has been broadcasted
    	}
    }

    public String getUserName() {
        return userName;
    }

    public int getHopCount() {
        return hopCount;
    }
}
