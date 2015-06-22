import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

public class DNConnection {

    private Socket clientSocket;
    MsgParser parser;
    private BufferedOutputStream bw;
    private boolean serverShutdown;

    private String userId;
    private String userName;

    public DNConnection(Socket clientSocket) {
        try {
            this.clientSocket = clientSocket;
            this.bw = new BufferedOutputStream(clientSocket.getOutputStream());
            System.out.println("[WS] Incoming socket!");
            this.parser = new MsgParser(clientSocket.getInputStream());
            this.serverShutdown = false;
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
                clientMessage.execute(this, bw, clientSocket);
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

    /*
     * Wait for client handshake and reply with respective message.
     */
    private void handshake() throws IOException, InterruptedException, NoSuchAlgorithmException {
        HTTPMsg clientHandshake = parser.getHTTPMessage();
        String serverHandshake = createHandshakeMessage(clientHandshake);
        PrintWriter pr = new PrintWriter(clientSocket.getOutputStream(), true);
        pr.print(serverHandshake);
        pr.flush();

        System.out.println("[WS] Handshake complete!");
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

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void sendMessage(SendMsg msg, String userId) throws IOException {
        String message = ChatMsgFactory.createResponse("SEND", msg.id, new String[]{ userId, msg.getMessage()});
        bw.write(FrameFactory.TextFrame(message));
        bw.flush();
    }
}