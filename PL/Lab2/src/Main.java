import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.xml.bind.DatatypeConverter;

public class Main {

    public static void main(String[] args) throws InterruptedException, NoSuchAlgorithmException {
    	//TODO: Make this a parameter? Research in specification
        int port = 4711;
        System.out.println("dnChat is getting started!");

        ServerSocket welcomeSocket;
        Socket clientSocket;
        PrintWriter pr;
        MsgParser msgParser;

        try {
            welcomeSocket = new ServerSocket(port);
            System.out.println("[WS] Socket bound on port " + port + ".");
            clientSocket = welcomeSocket.accept();
            InputStreamReader sr = new InputStreamReader(new DataInputStream(clientSocket.getInputStream()));
            pr = new PrintWriter(clientSocket.getOutputStream(), true);
            System.out.println("[WS] Incoming socket!");
            msgParser = new MsgParser(clientSocket.getInputStream());

            Message handshake = msgParser.getHTTPMessage();
            
            //TODO: Maybe make this a factory!
            String base64Token = getSecToken(handshake.WebSocketKey);
            String welcomeMsg = "HTTP/1.1 101 Switching Protocols\n" +
                    "Upgrade: websocket\n" +
                    "Connection: Upgrade\n" +
                    "Sec-WebSocket-Accept: " + base64Token;
            welcomeMsg += "\r\n\r\n";
            System.out.print(welcomeMsg);

            pr.print(welcomeMsg);
            pr.flush();

            System.out.println("[WS] Handshake complete!");

            while(true) {
                Message message2 = msgParser.getWebsocketMessage();
                System.out.println(message2);
                Thread.sleep(1000);
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    /*
     * TODO: What does this method do?
     */
    private static String getSecToken(String token) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        token += "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        MessageDigest cript = MessageDigest.getInstance("SHA-1");
        cript.reset();
        cript.update(token.getBytes("utf8"));
        return DatatypeConverter.printBase64Binary(cript.digest());
    }
}
