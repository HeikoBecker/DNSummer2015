import java.net.Socket;

public class ConnectionThread extends Thread {

    DNConnection connection;
    final Socket clientSocket;

    public ConnectionThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        connection = new DNConnection(clientSocket);
        connection.run();
    }

    public void tellShutdown() {
        this.connection.tellShutdown();
    }
}
