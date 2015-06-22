import java.io.IOException;
import java.net.Socket;

public class ConnectionThread extends Thread {

    DNConnection connection;
    final Socket clientSocket;

    public ConnectionThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            connection = new DNConnection(clientSocket);
            connection.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tellShutdown() {
        this.connection.tellShutdown();
    }
}
