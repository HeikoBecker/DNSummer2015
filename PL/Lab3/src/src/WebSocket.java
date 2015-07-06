import javax.xml.bind.DatatypeConverter;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class WebSocket {
    private static Random random = new Random();

    private Socket peerSocket;
    MsgParser parser;
    private BufferedOutputStream bw;

    public WebSocket(Socket peerSocket) throws IOException {
        try {
            this.peerSocket = peerSocket;
            this.bw = new BufferedOutputStream(peerSocket.getOutputStream());
            log("WebSocket created.");
            this.parser = new MsgParser(peerSocket.getInputStream());
        } catch (IOException e) {
            System.out.println(e.getMessage());
            close();
        }
    }

    public void close() {
        if (!isClosed()) {
            try {
                this.emitFrame(FrameFactory.CloseFrame(0));
                peerSocket.shutdownInput();
                peerSocket.shutdownOutput();
                peerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * Emitting a chat message, consisting of a command, an id and a list of additional lines.
     */
    public void emit(String command, String id, String[] lines) throws IOException {
        String message = ChatMsgCodec.encodeServerMessage(command, id, lines);
        this.emitFrame(FrameFactory.TextFrame(message));
    }

    /*
     * Emitting a frame of bytes to the client.
     */
    public void emitFrame(byte[] frame) throws IOException {
        bw.write(frame);
        bw.flush();
    }

    public boolean isClosed() {
        return peerSocket.isClosed();
    }

    public Message getWebsocketMessage() throws IOException {
        return parser.getWebsocketMessage();
    }


    public boolean awaitHandshake() throws IOException, NoSuchAlgorithmException, InterruptedException {
        log("Awaiting handshake.");

        HTTPMsg clientHandshake = parser.getHTTPMessage(false);
        PrintWriter pr = new PrintWriter(this.peerSocket.getOutputStream(), true);
        if ((clientHandshake.isInvalid() && clientHandshake.isHostSet()) || !clientHandshake.Type.equals("Handshake")) {
            String serverReply = createInvReply();
            pr.print(serverReply);
            pr.flush();
            log("Handshake failed due to client error.\n Closing connection.");
            return false;
        }
        String serverHandshake = createHandshakeResponseMessage(clientHandshake);
        pr.print(serverHandshake);
        pr.flush();
        log("Handshake completed.");
        return true;
    }

    /*
     * Create a Reply indicating that the handshake cannot be completed.
     * Taken from RFC 6455 Page 26 and 63
     */
    private String createInvReply() {
        return "HTTP/1.1 400 Bad Request\r\n"
                + "Sec-WebSocket-Version: 13\r\n"
                + "\r\n";
    }

    /*
     * Given the handshake message by the client, the servers handshake message is constructed.
     */
    private static String createHandshakeResponseMessage(HTTPMsg clientHandshake) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return "HTTP/1.1 101 Switching Protocols\n"
                + "Upgrade: websocket\n"
                + "Connection: Upgrade\n"
                + "Sec-WebSocket-Accept: " + WebSocket.getSecToken(clientHandshake.getWebSocketKey()) + "\r\n\r\n";
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

    public void executeHandshake(String host) throws IOException, InterruptedException {
        log("Sending handshake.");
        PrintWriter pr = new PrintWriter(this.peerSocket.getOutputStream(), true);
        byte[] nonce = new byte[16];
        random.nextBytes(nonce);
        String encodedNonce = DatatypeConverter.printBase64Binary(nonce);
        String handshake = "GET / HTTP/1.1\r\n" +
                "Host: " + host + "\r\n" +
                "Connection: Upgrade\r\n" +
                "Upgrade: websocket\r\n" +
                "Sec-WebSocket-Key: " + encodedNonce + "\r\n" +
                "Sec-WebSocket-Version: 13\r\n\r\n";
        pr.print(handshake);
        pr.flush();

        // used to read the response
        HTTPMsg msg = parser.getHTTPMessage(true); // TODO: ensure that the peer responded correctly
        log("Handshake completed.");
    }


    // ----------------- DEBUGGING -----------------
    private final boolean DEBUG = false;

    private void log(String msg) {
        if (DEBUG) {
            System.out.println("[WS] " + msg);
        }
    }
}
