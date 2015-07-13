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

    // TODO: merge these function together
    public void sendArrv(RemoteArrvChatMsg arrvChatMsg) throws IOException {
        log("Broadcast ARRV");
        this.emit(true, "ARRV", arrvChatMsg.getId(), new String[]{arrvChatMsg.getUserName(), "", ((arrvChatMsg.getHopCount() + 1) + "")});
    }

    public void sendArrv(String userId, String userName, String groupDescription, int hopCount) throws IOException {
        log("Broadcast ARRV");
        this.emit(true, "ARRV", userId, new String[]{userName, groupDescription, Integer.toString(hopCount)});
    }

    public void sendAckn(RemoteAcknChatMsg acknChatMsg) throws IOException {
        log("Broadcast ACKN");
        this.emit(true, "ACKN", acknChatMsg.getId(), new String[]{acknChatMsg.getAcknUserId(), acknChatMsg.getSenderUserId()});
    }


    public void sendLeft(String userId) throws IOException {
        this.emit(true, "LEFT", userId);
    }

    //TODO: Except for the flag, this method is a copy of the Clients emitMessage method
    public void emitMessage(LocalSendChatMsg msg, String senderId) throws IOException {
        this.websocket.emit(true, "SEND", msg.id, new String[]{msg.getRecipient(), senderId, msg.getMessage()});
        this.log("Broadcasted a message.");
    }

    public void emitAcknowledgement(LocalAcknChatMsg msg, String acknUserId, String senderUserId) throws IOException {
        this.websocket.emit(true, "ACKN", msg.id, new String[]{acknUserId, senderUserId});
        this.log("Broadcasted an ack.");
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

    public void registerClient(RemoteClient remoteClient) {
        this.clients.put(remoteClient.getUserId(), remoteClient);
    }
}
