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
        } catch (IOException e) {
            System.out.println(e.getMessage());
            exit();
        }
    }

    public void run() throws IOException {
        try {
            if (handshake()){
            	while (!clientSocket.isClosed() && !this.serverShutdown) {
                // Let new message execute, resp. send messages on socket
            		Message websocketMessage = parser.getWebsocketMessage();
            		if(websocketMessage == null) {
            			exit();
            			break;
            		}
            		websocketMessage.execute(this);
            	}
            }
        } catch (SocketException | InterruptedException | NoSuchAlgorithmException e) {
            System.out.println(e.getMessage());
        } finally {
            exit();
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

    public boolean isClosed() {
        return this.clientSocket.isClosed();
    }

    public void tellShutdown() {
        synchronized (this) {
            this.serverShutdown = true;
        }
    }


    // ----------------- HTTP HANDSHAKE -----------------

    /*
     * Wait for client handshake and reply with respective message.
     * Section 4.2.1 May need to be included here with the parser and the set method in HTTPMsg
     */
    private boolean handshake() throws IOException, InterruptedException, NoSuchAlgorithmException {
    	System.out.println("Parsing message");
        HTTPMsg clientHandshake = parser.getHTTPMessage();
    	PrintWriter pr = new PrintWriter(clientSocket.getOutputStream(), true);
    	if(clientHandshake.isInvalid() || clientHandshake.Type != "Handshake"){
        	String serverReply = createInvReply(clientHandshake);
        	pr.print(serverReply);
        	pr.flush();
        	log("[WS] Handshake failed due to client error.\n Closing connection.");
        	return false;
    	}
        String serverHandshake = createHandshakeMessage(clientHandshake);
       	pr.print(serverHandshake);
       	pr.flush();
       	log("[WS] Handshake complete!");
       	return true;
    }

    /*
     * Create a Reply indicating that the handshake cannot be completed.
     * TODO: Does this suffice?
     */
    private String createInvReply(HTTPMsg clientHandshake) {
		return "HTTP/1.1 400 Bad Request\r\n\r\n";
	}

	/*
     * Given the handshake message by the client, the servers handshake message is constructed.
     */
    private static String createHandshakeMessage(HTTPMsg clientHandshake) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return "HTTP/1.1 101 Switching Protocols\n"
                + "Upgrade: websocket\n" + "Connection: Upgrade\n"
                + "Sec-WebSocket-Accept: " + Client.getSecToken(clientHandshake.getWebSocketKey()) + "\r\n\r\n";
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


    // ----------------- ENTER / LEAVE -----------------

    /*
     * Given userId and userName, the current connection can enter authenticated state.
     */
    public void authenticate(String userId, String userName) throws IOException {
        isAuthenticated = true;
        this.userId = userId;
        this.userName = userName;
        Chat.getInstance().registerClient(this);
    }

    /*
     * When a client exits, other client should be informed and the socket should be shut down properly.
     */
    public void exit() throws IOException {
        if(isAuthenticated) {
            Chat.getInstance().unregisterClient(userId);
        }
        if(!isClosed()) {
            // TODO: 0 might be replaced by real reason
            this.emitFrame(FrameFactory.CloseFrame(0));
            clientSocket.shutdownInput();
            clientSocket.shutdownOutput();
            clientSocket.close();
        }
    }


    // ----------------- EMIT AND RECEIVE METHODS -----------------

    /*
     * Emitting another client's message to the current client.
     */
    public void emitSendChatMsg(SendChatMsg msg, String senderId) throws IOException {
        emit("SEND", msg.id, new String[]{senderId, msg.getMessage()});
    }

    /*
     * Emitting another client's ackn message to the current client.
     */
    public void emitAcknChatMsg(AcknChatMsg msg, String senderId) throws IOException {
        emit("ACKN", msg.id, new String[]{senderId});
    }

    /*
     * Emitting that another client arrived to the current client.
     */
    public void emitArrvChatMsg(Client otherClient) throws IOException {
        emit("ARRV", otherClient.getUserId(), new String[]{otherClient.getUserName(), ""});
    }

    /*
     * Emitting that another client left to the current client.
     */
    public void emitLeftChatMsg(String otherClient) throws IOException {
        emit("LEFT", otherClient, new String[]{});
    }

    /*
     * Current client sent an ackn message, which is sent to the other clients.
     */
    public void recvAcknChatMsg(AcknChatMsg acknMsg) throws IOException {
        Chat.getInstance().emitAcknowledgement(acknMsg, this);
    }

    /*
     * Current client sent a message, which is sent to the other clients.
     */
    public void recvSendChatMsg(SendChatMsg sendMsg) throws IOException {
        Chat.getInstance().emitMessage(sendMsg, this);
    }

    /*
     * Emitting a chat message, consisting of a command, an id and a list of additional lines.
     */
    public void emit(String command, String id, String[] lines) throws IOException {
        String message = ChatMsgCodec.encodeServerMessage(command, id, lines);
        this.emitFrame(FrameFactory.TextFrame(message));
    }

    /*
     * Emitting a chat message, consisting of a command, an id and no other lines.
     */
    public void emit(String command, String id) throws IOException {
        this.emit(command, id, new String[]{});
    }

    /*
     * Emitting a frame of bytes to the client.
     */
    public void emitFrame(byte[] frame) throws IOException {
        bw.write(frame);
        bw.flush();
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
        final Client other = (Client) obj;

        return !((this.userId == null) ? (other.userId != null) : !this.userId.equals(other.userId));
    }

    // ----------------- DEBUGGING -----------------
    private final boolean DEBUG = true;

    private void log(String msg) {
        if (DEBUG) {
            System.out.println(msg);
        }
    }
}