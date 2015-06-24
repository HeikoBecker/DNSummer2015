import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

public class DNConnection {
    private final boolean DEBUG = false;

    private Socket clientSocket;
    MsgParser parser;
    private BufferedOutputStream bw;
    private boolean serverShutdown;

    private String userId;
    private String userName;
    private boolean isAuthenticated = false;

    public DNConnection(Socket clientSocket) throws IOException {
        try {
            this.clientSocket = clientSocket;
            this.bw = new BufferedOutputStream(clientSocket.getOutputStream());
            log("[WS] Incoming socket!");
            this.parser = new MsgParser(clientSocket.getInputStream());
            this.serverShutdown = false;
        } catch (SocketException e) {
            System.out.println(e.getMessage());
            System.out.println(e.getCause());
            DNChat.getInstance().closeConnection(this.userId);
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public void run() {
        try {
            handshake();
            while (!clientSocket.isClosed() && !this.serverShutdown) {
                Message clientMessage = parser.getWebsocketMessage(userId);
                // Let new message execute, resp. send messages on socket
                clientMessage.execute(this);
            }
        } catch (IOException | InterruptedException | NoSuchAlgorithmException e) {
            System.out.println(e);
        }
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

    /*
     * Wait for client handshake and reply with respective message.
     */
    private void handshake() throws IOException, InterruptedException, NoSuchAlgorithmException {
        HTTPMsg clientHandshake = parser.getHTTPMessage();
        String serverHandshake = createHandshakeMessage(clientHandshake);
        PrintWriter pr = new PrintWriter(clientSocket.getOutputStream(), true);
        pr.print(serverHandshake);
        pr.flush();

        log("[WS] Handshake complete!");
    }

    /*
     * Given the handshake message by the client, the servers handshake message is constructed.
     */
    private static String createHandshakeMessage(HTTPMsg clientHandshake) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        String base64Token = DNConnection.getSecToken(clientHandshake.WebSocketKey);
        String message = "HTTP/1.1 101 Switching Protocols\n"
                + "Upgrade: websocket\n" + "Connection: Upgrade\n"
                + "Sec-WebSocket-Accept: " + base64Token + "\r\n\r\n";
        return message;
    }

    public void tellShutdown() {
        synchronized (this) {
            this.serverShutdown = true;
        }
    }

    /*
     * The Sec-Websocket-Key is processed and converted as stated in RFC6455. Therefore it is concatenated,
     * the SHA-1 hash is taken and the resulting bytes are converted to base64.
     */
    private static String getSecToken(String token)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        token += "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        MessageDigest cript = MessageDigest.getInstance("SHA-1");
        cript.reset();
        cript.update(token.getBytes("utf8"));
        return DatatypeConverter.printBase64Binary(cript.digest());
    }

    public void sendMessage(SendMsg msg, String userId) throws IOException {
        send("SEND", msg.id, new String[]{userId, msg.getMessage()});
    }

    public void sendAckn(AcknMsg msg, String userId) throws IOException {
        send("ACKN", msg.id, new String[]{userId});
    }

    public void sendArrv(String userId, String userName) throws IOException {
        send("ARRV", userId, new String[]{userName, "Desc"});
    }

    public void sendLeft(String userId) throws IOException {
        send("LEFT", userId, new String[]{});
    }

    /* Helpers */
    public void send(String command, String id, String[] lines) throws IOException {
        String message = ChatMsgFactory.createResponse(command, id, lines);
        bw.write(FrameFactory.TextFrame(message));
        bw.flush();
    }

    public void send(String command, String id) throws IOException {
        this.send(command, id, new String[]{});
    }

    public void sendFrame(byte[] frame) throws IOException {
        bw.write(frame);
        bw.flush();
    }

    private void log(String msg) {
        if (DEBUG) {
            System.out.println(msg);
        }
    }

    public void authenticate(String id, String name) throws IOException {
        isAuthenticated = true;
        this.userId = id;
        this.userName = name;
        DNChat.getInstance().addConnection(id, this);
    }

    public void close() throws IOException {
        DNChat.getInstance().closeConnection(userId);
        clientSocket.shutdownInput();
        clientSocket.shutdownOutput();
        clientSocket.close();
    }

    public void recvAckn(AcknMsg acknMsg) throws IOException {
        DNChat.getInstance().sendAcknowledgement(acknMsg, this);
    }

    public void recvMsg(SendMsg sendMsg) throws IOException {
        DNChat.getInstance().sendMessage(sendMsg, this);
    }
}