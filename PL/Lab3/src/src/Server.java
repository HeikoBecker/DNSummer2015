import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;

public class Server extends Peer {
    private static int maxId = 0;
    private int id;
    private HashMap<String, RemoteClient> clients = new HashMap<>();
    private boolean exited = false;

    public Server(Peer peer) {
        this.websocket = peer.websocket;
        this.id = maxId++;
    }

    public Server(Socket socket) throws IOException {
        super(socket);
        this.id = maxId++;
    }

    public void connect(String host) throws IOException {
        if (this.websocket.executeHandshake(host)){
        	this.emit(true, "SRVR", "0");
        	log("SRVR segment sent.");
        	//tell the other server about all clients that connected to the local instance
        	Chat.getInstance().advertiseCurrentUsers(this);
        }else{
        	//close the websocket such that "run" will terminate
        	this.websocket.close();
        	this.exited = true;
        }
    }

    @Override
    public void exit() {
        if(!exited) {
            Chat.getInstance().removeFederationServer(this);

            // TODO: ensure proper clean-up, by removing all entries entered by this instance
            this.websocket.close();
            exited = true;
        }
    }


    // TODO: merge these functions!
    public void emitAckn(RemoteAcknChatMsg acknChatMsg) throws IOException {
        this.emit(true, "ACKN", acknChatMsg.getId(), new String[]{acknChatMsg.getAcknUserId(), acknChatMsg.getSenderUserId()});
        this.log("Forward an ACKN.");
    }

    public void emitAckn(LocalAcknChatMsg msg, String acknUserId, String senderUserId) throws IOException {
        this.emit(true, "ACKN", msg.id, new String[]{acknUserId, senderUserId});
        this.log("Forward an ACKN.");
    }

    /*
     * Forward a SEND message to this server.
     */
    public void emitSend(LocalSendChatMsg msg, String senderId) throws IOException {
        this.emit(true, "SEND", msg.id, new String[]{msg.getRecipient(), senderId, msg.getMessage()});
        this.log("Forward a SEND.");
    }

    /*
     * Forward a ARRV message to this server.
     */
    public void emitArrv(String userId, String userName, String groupDescription, int hopCount) throws IOException {
        log("Forward an ARRV: " + userId + " (" + userName + ", " + groupDescription + ", " + hopCount + ")");
        this.emit(true, "ARRV", userId, new String[]{userName, groupDescription, Integer.toString(hopCount)});
    }

    /*
     * Forward a LEFT message to this server.
     */
    public void emitLeft(String userId) throws IOException {
        this.emit(true, "LEFT", userId);
        this.log("Forward a LEFT.");
    }

    /*
     * The following methods are used to handle the remote clients connected to a server.
     */
    public void registerClient(RemoteArrvChatMsg remoteClient) { this.clients.put(remoteClient.getId(), new RemoteClient(remoteClient.getId(), remoteClient.getUserName(), remoteClient.getDescription(), remoteClient.getHopCount())); }
    public void unregisterClient(String id) { this.clients.remove(id); }

    public RemoteClient getClient(String clientId) {
        return clients.get(clientId);
    }

    public LinkedList<RemoteClient> getClients() {
        return new LinkedList<>(clients.values());
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
        final Server other = (Server) obj;

        return this.id == other.id;
    }


    // ----------------- DEBUGGING -----------------
    private final boolean DEBUG = true;

    protected void log(String msg) {
        if (DEBUG) {
            System.out.println("[S] " + msg);
        }
    }

	@Override
	public void authenticate(String userId, String userName) throws IOException {
		// TODO Auto-generated method stub	
	}

	@Override
	public boolean isAuthenticated() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void recvAcknChatMsg(LocalAcknChatMsg acknMsg) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void recvSendChatMsg(LocalSendChatMsg sendMsg) throws IOException {
		// TODO Auto-generated method stub
		
	}

	public boolean isFailed(){
		return this.exited;
	}
}
