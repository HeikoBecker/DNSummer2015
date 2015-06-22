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
    PrintWriter pr;
    MsgParser parser;
    private BufferedOutputStream bw;
    private boolean serverShutdown;

    public DNConnection(Socket clientSocket) {
        try {
            this.clientSocket = clientSocket;
            this.pr = new PrintWriter(clientSocket.getOutputStream(), true);
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

            Message clientHandshake = parser.getHTTPMessage();
            String serverHandshake = createHandshakeMessage(clientHandshake);
            pr.print(serverHandshake);
            pr.flush();

            System.out.println("[WS] Handshake complete!");

            while (!clientSocket.isClosed() && !this.serverShutdown) {
                Message clientMessage = parser.getWebsocketMessage();
                // Let new message execute, resp. send messages on socket
                clientMessage.execute(bw, pr, clientSocket);
            }
        } catch (IOException | InterruptedException | NoSuchAlgorithmException e) {
            System.out.println(e);
        }
    }

    /*
     * Given the handshake message by the client, the servers handshake message is constructed.
     */
    private String createHandshakeMessage(Message clientHandshake) throws NoSuchAlgorithmException, UnsupportedEncodingException {
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
}