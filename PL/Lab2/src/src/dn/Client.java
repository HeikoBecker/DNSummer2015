package dn;

import dn.messages.*;
import dn.messages.chat.AcknChatMsg;
import dn.messages.chat.SendChatMsg;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

public class Client {
    private final boolean DEBUG = false;

    private Socket clientSocket;
    MsgParser parser;
    private BufferedOutputStream bw;
    private boolean serverShutdown;

    private String userId;
    private String userName;
    private boolean isAuthenticated = false;

    // TODO: should a connection close after some time??? Make use of PONG dn.messages?

    public Client(Socket clientSocket) throws IOException {
        try {
            this.clientSocket = clientSocket;
            this.bw = new BufferedOutputStream(clientSocket.getOutputStream());
            log("[WS] Incoming socket!");
            this.parser = new MsgParser(clientSocket.getInputStream());
            this.serverShutdown = false;
        } catch (SocketException e) {
            System.out.println(e.getMessage());
            Chat.getInstance().unregisterClient(this.userId);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public void run() throws IOException {
        try {
            handshake();
            while (!clientSocket.isClosed() && !this.serverShutdown) {
                // Let new message execute, resp. send dn.messages on socket
                parser.getWebsocketMessage().execute(this);
            }
        } catch (SocketException | InterruptedException e) {
            System.out.println(e.getMessage());
            Chat.getInstance().unregisterClient(this.userId);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
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
        return "HTTP/1.1 101 Switching Protocols\n"
                + "Upgrade: websocket\n" + "Connection: Upgrade\n"
                + "Sec-WebSocket-Accept: " + Client.getSecToken(clientHandshake.WebSocketKey) + "\r\n\r\n";
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

    public void emitSendChatMsg(SendChatMsg msg, String userId) throws IOException {
        send("SEND", msg.id, new String[]{userId, msg.getMessage()});
    }

    public void emitAcknChatMsg(AcknChatMsg msg, String userId) throws IOException {
        send("ACKN", msg.id, new String[]{userId});
    }

    public void emitArrvChatMsg(Client otherClient) throws IOException {
        send("ARRV", otherClient.getUserId(), new String[]{otherClient.getUserName(), "Desc"});
    }

    public void sendLeft(String userId) throws IOException {
        send("LEFT", userId, new String[]{});
    }

    public void recvAckn(AcknChatMsg acknMsg) throws IOException {
        Chat.getInstance().sendAcknowledgement(acknMsg, this);
    }

    public void recvMsg(SendChatMsg sendMsg) throws IOException {
        Chat.getInstance().sendMessage(sendMsg, this);
    }

    /* Helpers */
    public void send(String command, String id, String[] lines) throws IOException {
        String message = ChatMsgCodec.encodeServerMessage(command, id, lines);
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
        Chat.getInstance().registerClient(this);
    }

    public void close() throws IOException {
        Chat.getInstance().unregisterClient(userId);
        clientSocket.shutdownInput();
        clientSocket.shutdownOutput();
        clientSocket.close();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Client other = (Client) obj;

        return !((this.userId == null) ? (other.userId != null) : !this.userId.equals(other.userId));
    }
}