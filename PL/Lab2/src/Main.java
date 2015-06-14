import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        int port = 4711;
        System.out.println("dnChat is getting started!");

        ServerSocket welcomeSocket;
        Socket clientSocket;
        DataInputStream is;
        PrintStream os;
        String line;
        try {
            welcomeSocket = new ServerSocket(port);
            System.out.println("Socket bound on port " + port + ".");
            clientSocket = welcomeSocket.accept();
            is = new DataInputStream(clientSocket.getInputStream());
            os = new PrintStream(clientSocket.getOutputStream());
            System.out.println("Incoming socket!");
            while (true) {
                System.out.println("Waiting for messages");
                line = is.readLine();
                System.out.println(line);
                os.println(line);
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}
