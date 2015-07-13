import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;

public class Server extends Peer {
    private static int maxId = 0;
    private int id;
    private HashMap<String, RemoteClient> clients = new HashMap<>();

    public Server(Peer peer) {
        this.websocket = peer.websocket;
        this.id = maxId++;
    }

    public Server(Socket socket) throws IOException {
        super(socket);
        this.id = maxId++;
    }

    public void connect(String host) throws IOException {
        this.websocket.executeHandshake(host);
        this.emit(true, "SRVR", "0");
        log("SRVR segment sent.");
        // tell the other server about all clients that connected to the local instance
        Chat.getInstance().advertiseCurrentUsers(this);
    }

    @Override
    public void exit() {
        log("SERVER EXITING");
        // TODO: ensure proper clean-up, by removing all entries entered by this instance
        this.websocket.close();
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
    public void registerClient(RemoteClient remoteClient) {
        this.clients.put(remoteClient.getUserId(), remoteClient);
    }

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
}
