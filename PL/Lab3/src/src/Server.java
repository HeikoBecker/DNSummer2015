import java.io.IOException;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;

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

    public void connect(String host) throws IOException, InterruptedException {
        this.websocket.executeHandshake(host);
        this.emit(true, "SRVR", "0");
        log("SRVR segment sent.");

        // TODO: tell the other server about all clients that connected to the local instance
        Chat.getInstance().advertiseCurrentUsers(this);
    }

    @Override
    public void exit() throws IOException {
        log("SERVER EXITING");

        // TODO: ensure proper clean-up, by removing all entries entered by this instance

        this.websocket.close();
    }

    // TODO: merge these function together
    public void sendArrv(ArrvChatMsg arrvChatMsg) throws IOException {
        log("Broadcast ARRV");
        this.emit(true, "ARRV", arrvChatMsg.getId(), new String[]{arrvChatMsg.getUserName(), "", ((arrvChatMsg.getHopCount() + 1) + "")});
    }

    public void sendArrv(String userId, String userName, String groupDescription, int hopCount) throws IOException {
        log("Broadcast ARRV");
        this.emit(true, "ARRV", userId, new String[]{userName, groupDescription, Integer.toString(hopCount) });
    }

    public void sendLeft(String userId) throws IOException {
        this.emit(true, "LEFT", userId);
    }

    public Collection<RemoteClient> getClients() {
        return clients.values();
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
