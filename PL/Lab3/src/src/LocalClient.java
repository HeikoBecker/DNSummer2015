import java.io.IOException;

/*
 * A LocalClient encapsulates the associated port for the connection as well as the user information.
 */
public class LocalClient extends Peer {
    // Chat Protocol Level
    private String userId = "";
    private String userName;
    private boolean isAuthenticated = false;
    private int hopCount = 0;

    public LocalClient(Peer peer) {
        this.websocket = peer.websocket;
        this.websocket.setClient();
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
    public void exit() throws IOException {
        if (isAuthenticated) {
            Chat.getInstance().unregisterClient(userId);
        }
        this.log("Exited.");
        this.websocket.close();
    }


    // ----------------- EMIT METHODS -----------------

    /*
     * Emitting another client's message to the current client.
     */
    public void emitSendChatMsg(SendChatMsg msg, String senderId) throws IOException {
        this.websocket.emit(false, "SEND", msg.id, new String[]{senderId, msg.getMessage()});
        this.log("Received a message.");
    }

    /*
     * Emitting another client's ackn message to the current client.
     */
    public void emitAcknChatMsg(AcknChatMsg msg, String senderId) throws IOException {
        this.websocket.emit(false, "ACKN", msg.id, new String[]{senderId});
        this.log("Received an ack.");
    }

    /*
     * Emitting that another client arrived to the current client.
     * Sending an empty description string is ok, as stated here: https://dcms.cs.uni-saarland.de/dn/forum/viewtopic.php?f=3&t=132
     */
    public void emitArrvChatMsg(LocalClient otherClient) throws IOException {
        emitArrvChatMsg(otherClient.getUserId(), otherClient.getUserName());
    }

    /*
     * Emitting that another client arrived to the current client.
     * Sending an empty description string is ok, as stated here: https://dcms.cs.uni-saarland.de/dn/forum/viewtopic.php?f=3&t=132
     */
    public void emitArrvChatMsg(String userId, String userName) throws IOException {
        this.websocket.emit(false, "ARRV", userId, new String[]{ userName, ""});
        this.log("Received an arrv.");
    }

    /*
     * Emitting that another client left to the current client.
     */
    public void emitLeftChatMsg(String otherClient) throws IOException {
        this.websocket.emit(false, "LEFT", otherClient, new String[]{});
        this.log("Received a left.");
    }

    // ----------------- RECEIVE METHODS -----------------
    /*
     * Current client sent an ackn message, which is sent to the other clients.
     */
    @Override
    public void recvAcknChatMsg(AcknChatMsg acknMsg) throws IOException {
        Chat.getInstance().emitAcknowledgement(acknMsg, this);
        this.log("Sent an ack.");
    }

    /*
     * Current client sent a message, which is sent to the other clients.
     */
    @Override
    public void recvSendChatMsg(SendChatMsg sendMsg) throws IOException {
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
    	return "LocalClient "+this.userName+", HOPS:"+this.hopCount;
    }
    
    @Override
    protected void log(String msg) {
        if (DEBUG) {
            String userId = (this.userId.equals("")) ? "UNAUTH" : this.userId;
            System.out.println("[C-" + userId + "] " + msg);
        }
    }

    public int getHopCount() {
        return hopCount;
    }

    public void setHopCount(int hopCount) {
        this.hopCount = hopCount;
    }
}