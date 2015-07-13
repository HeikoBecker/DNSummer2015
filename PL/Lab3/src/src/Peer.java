import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;

public class Peer {
    // Network Level
    protected WebSocket websocket;
    private int hopCount;

    public Peer() {
        websocket = null;
    }

    public Peer(Socket peerSocket) throws IOException {
        this.websocket = new WebSocket(peerSocket);
    }

    public Message initialize() throws IOException {
        try {
            if (websocket.awaitHandshake()) {
                Message websocketMessage = this.websocket.getWebsocketMessage();
                if (websocketMessage == null) {
                    exit();
                }
                return websocketMessage;
            }
        } catch (SocketException | NoSuchAlgorithmException | InterruptedException e) {
            System.out.println(e.getMessage());
        }
        return new CloseConnMsg();
    }

    public void run() throws IOException {
        while (!websocket.isClosed()) {
            // Let new message execute, resp. send messages on socket
            Message websocketMessage = this.websocket.getWebsocketMessage();
            if (websocketMessage == null) {
                break;
            }
            websocketMessage.execute(this);
        }
        exit();
    }

    // ----------------- EMIT METHODS TO FORWARD TO SOCKET -----------------
    public void emit(boolean asClient, String command, String id, String[] lines) throws IOException {
        this.websocket.emit(asClient, command, id, lines);
    }

    public void emit(boolean asClient, String command, String id) throws IOException {
        this.emit(asClient, command, id, new String[]{});
    }

    public void emitFrame(byte[] frame) throws IOException {
        this.websocket.emitFrame(frame);
    }

    // ----------------- ENTER / LEAVE -----------------

    public void authenticate(String userId, String userName) throws IOException {
        throw new NotImplementedException();
    }

    public boolean isAuthenticated() {
        throw new NotImplementedException();
    }

    // ----------------- RECEIVE METHODS -----------------

    public void recvAcknChatMsg(LocalAcknChatMsg acknMsg) throws IOException {
        throw new NotImplementedException();
    }

    public void recvSendChatMsg(LocalSendChatMsg sendMsg) throws IOException {
        throw new NotImplementedException();
    }

    public void connect(String host) throws IOException, InterruptedException {
        throw new NotImplementedException();
    }

    public void exit() throws IOException {
        throw new NotImplementedException();
    }

    // ----------------- DEBUGGING -----------------
    protected final boolean DEBUG = false;

    protected void log(String msg) {
        if (DEBUG) {
            System.out.println("[P] " + msg);
        }
    }
}
