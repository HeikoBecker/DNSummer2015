import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import javax.xml.bind.DatatypeConverter;

public class Main {

    public static void main(String[] args) throws InterruptedException, NoSuchAlgorithmException {
        int port = 4711;
        System.out.println("dnChat is getting started!");

        ServerSocket welcomeSocket;
        Socket clientSocket;
        OutputStream os;
        Message line;
        MsgParser parser;

        boolean didHandshake = false;

        try {
            welcomeSocket = new ServerSocket(port);
            System.out.println("Socket bound on port " + port + ".");
            clientSocket = welcomeSocket.accept();
            parser = new MsgParser(clientSocket.getInputStream());
            os = clientSocket.getOutputStream();
            System.out.println("Incoming socket!");
            while (true) {
                line = parser.getMsg();
                if (!didHandshake && Objects.equals(line.Type, "Handshake")) {
                    String token = line.WebSocketKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
                    MessageDigest cript = MessageDigest.getInstance("SHA-1");
                    cript.reset();
                    cript.update(token.getBytes("utf8"));
                    String welcomeMsg = "HTTP/1.1 101 Switching Protocols\n" +
                            "Upgrade: websocket\n" +
                            "Connection: Upgrade\n" +
                            "Sec-WebSocket-Accept: " + DatatypeConverter.printBase64Binary(cript.digest());
                    System.out.println(welcomeMsg);
                    os.write(welcomeMsg.getBytes());
                    os.flush();
                    didHandshake = true;
                } else {
                    System.out.println(line);

                }
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}
