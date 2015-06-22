import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

public class WelcomeThread implements Runnable {
    LinkedList<ConnectionThread> connections = new LinkedList<ConnectionThread>();

    public void tellShutdown() {
        for (ConnectionThread t : this.connections) {
            t.tellShutdown();
        }
    }

    @Override
    public void run() {
        int port = 42015; // using the dnChat protocol's default port.

        try {
            ServerSocket socket = new ServerSocket(port);
            System.out.println("[WS] Welcome Socket bound on port " + port + ".");

            while (true) {
                final Socket clientSocket = socket.accept();
                ConnectionThread conn = new ConnectionThread(clientSocket);
                conn.start();
                this.connections.add(conn);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
