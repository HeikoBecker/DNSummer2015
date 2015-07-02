import java.io.IOException;
import java.net.Socket;

/*
 * @class ConnectionThread
 * A Thread to handle communication on a single TCP connection between a client 
 * and a server.
 */
public class ConnectionThread extends Thread {
    private final boolean DEBUG = false;

    Client connection;
    final Socket clientSocket;

    public ConnectionThread(Socket clientSocket) {
        if (DEBUG) {
            System.out.println("[TCP] New connection established");
        }
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            // create a new client and let it execute
            connection = new Client(clientSocket);
            connection.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
