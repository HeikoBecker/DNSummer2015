import java.io.IOException;
import java.net.ServerSocket;
import java.util.LinkedList;

public class WelcomeThread implements Runnable {
    LinkedList<ConnectionThread> connections = new LinkedList<>();


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
                ConnectionThread conn = new ConnectionThread(socket.accept());
                conn.start();
                this.connections.add(conn);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
