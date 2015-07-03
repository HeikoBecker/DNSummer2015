import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;

public class Peer {
    // Network Level
    protected WebSocket websocket;

    public Peer() {
        websocket = null;
    }

    public Peer(Socket clientSocket) throws IOException {
        this.websocket = new WebSocket(clientSocket);
    }

    public Message initialize() throws IOException {
        try {
            if (websocket.handshake()) {
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
        try {
            while (!websocket.isClosed()) {
                // Let new message execute, resp. send messages on socket
                Message websocketMessage = this.websocket.getWebsocketMessage();
                if (websocketMessage == null) {
                    exit();
                    break;
                }
                websocketMessage.execute(this);
            }
        } catch (SocketException e) {
            System.out.println(e.getMessage());
        } finally {
            exit();
        }
    }

    // ----------------- EMIT METHODS TO FORWARD TO SOCKET -----------------
    public void emit(String command, String id, String[] lines) throws IOException {
        this.websocket.emit(command, id, lines);
    }

    public void emit(String command, String id) throws IOException {
        this.emit(command, id, new String[]{});
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

    public void recvAcknChatMsg(AcknChatMsg acknMsg) throws IOException {
        throw new NotImplementedException();
    }

    public void recvSendChatMsg(SendChatMsg sendMsg) throws IOException {
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
