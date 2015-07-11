import java.io.IOException;
import java.net.Socket;

public class Server extends Peer {
    public Server(Peer peer) {
        this.websocket = peer.websocket;
    }

    public Server(Socket socket) throws IOException {
        super(socket);
    }

    @Override
    public void connect(String host) throws IOException, InterruptedException {
        this.websocket.executeHandshake(host);
        this.emit(true, "SRVR", "0");
        log("SRVR segment sent.");

        // TODO: tell the other server about all clients that connected to the local instance
        Chat.getInstance().broadcastUsers(this);
    }

    @Override
    public void exit() throws IOException {
        log("SERVER EXITING");

        // TODO: ensure proper clean-up, by removing all entries entered by this instance

        this.websocket.close();
    }

    public void sendArrv(Client newClient) throws IOException {
        log("Broadcast ARRV");
        this.emit(true, "ARRV", newClient.getUserId(), new String[]{newClient.getUserName(), "", "0"});
    }

    public void sendLeft(String userId) throws IOException {
        this.emit(true, "LEFT", userId);
    }

    // ----------------- DEBUGGING -----------------
    private final boolean DEBUG = true;

    protected void log(String msg) {
        if (DEBUG) {
            System.out.println("[S] " + msg);
        }
    }
}
