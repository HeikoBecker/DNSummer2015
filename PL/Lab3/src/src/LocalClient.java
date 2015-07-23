import java.io.IOException;
import java.net.Socket;

/*
 * A LocalClient encapsulates the associated port for the connection as well as the user information.
 */
public class LocalClient extends Peer {
    // Chat Protocol Level
    private String userId = "";
    private String userName;
    private boolean isAuthenticated = false;

    public LocalClient(Peer peer) {
        this.websocket = peer.websocket;
        this.websocket.setDnClient();
    }

	public LocalClient(Socket peerSocket) throws IOException {
		super(peerSocket);
	}

	public String getUserId() {
        return this.userId;
    }

    public String getUserName() {
        return this.userName;
    }

    public boolean isAuthenticated() {
        return this.isAuthenticated;
    }

    // ----------------- ENTER / LEAVE -----------------

    /*
     * Given userId and userName, the current connection can enter authenticated state.
     */
    public void authenticate(String userId, String userName) throws IOException {
        this.isAuthenticated = true;
        this.userId = userId;
        this.userName = userName;
        Chat.getInstance().registerClient(this);
        this.log("Authenticated.");
    }

    /*
     * When a client exits, other client should be informed and the socket should be shut down properly.
     */
    public void exit()  {
        if (isAuthenticated) {
            try {
                Chat.getInstance().unregisterClient(userId);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.log("Exited.");
        this.websocket.close();
    }


    // ----------------- EMIT METHODS -----------------

    /*
     * Emitting another client's message to the current client.
     */
    public void emitSendChatMsg(LocalSendChatMsg msg, String senderId) throws IOException {
        this.emit(false, "SEND", msg.id, new String[]{senderId, msg.getMessage()});
        this.log("Received a message.");
    }

    /*
     * Emitting another client's ackn message to the current client.
     */
    public void emitAcknChatMsg(LocalAcknChatMsg msg, String senderId) throws IOException {
        this.emit(false, "ACKN", msg.id, new String[]{senderId});
        this.log("Received an ack.");
    }

    public void emitAcknChatMsg(String messageId, String senderId) throws IOException {
        this.emit(false, "ACKN", messageId, new String[]{senderId});
        this.log("Received an ack.");
    }

    /*
     * Emitting that another client arrived to the current client.
     * Sending an empty description string is ok, as stated here: https://dcms.cs.uni-saarland.de/dn/forum/viewtopic.php?f=3&t=132
     */
    public void emitArrvChatMsg(LocalClient otherClient) throws IOException {
        emitArrvChatMsg(otherClient.getUserId(), otherClient.getUserName(), "Group 25");
    }

    /*
     * Emitting that another client arrived to the current client.
     * Sending an empty description string is ok, as stated here: https://dcms.cs.uni-saarland.de/dn/forum/viewtopic.php?f=3&t=132
     */
    public void emitArrvChatMsg(String userId, String userName, String description) throws IOException {
        this.emit(false, "ARRV", userId, new String[]{ userName, description});
        this.log("Received an arrv.");
    }

    /*
     * Emitting that another client left to the current client.
     */
    public void emitLeftChatMsg(String otherClient) throws IOException {
        this.emit(false, "LEFT", otherClient, new String[]{});
        this.log("Received a left.");
    }

    // ----------------- RECEIVE METHODS -----------------
    /*
     * Current client sent an ackn message, which is sent to the other clients.
     */
    @Override
    public void recvAcknChatMsg(LocalAcknChatMsg acknMsg) throws IOException {
        Chat.getInstance().emitAcknowledgement(acknMsg, this);
        this.log("Sent an ack.");
    }

    /*
     * Current client sent a message, which is sent to the other clients.
     */
    @Override
    public void recvSendChatMsg(LocalSendChatMsg sendMsg) throws IOException {
        Chat.getInstance().emitMessage(sendMsg, this.userId);
        this.log("Sent a message.");
    }

    /*
     * Used to compare clients and check whether they are the same or not.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final LocalClient other = (LocalClient) obj;

        return !((this.userId == null) ? (other.userId != null) : !this.userId.equals(other.userId));
    }

    /*
     * (non-Javadoc)
     * @see Peer#log(java.lang.String)
     * 
     */
    @Override
    public String toString(){
    	return "LocalClient "+this.userName;
    }
    
    @Override
    protected void log(String msg) {
        if (DEBUG) {
            String userId = (this.userId.equals("")) ? "UNAUTH" : this.userId;
            System.out.println("[C-" + userId + "] " + msg);
        }
    }
}