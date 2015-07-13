import java.io.IOException;
import java.net.ServerSocket;
import java.util.LinkedList;

/*
 * @class WelcomeThread
 * Welcomes new arriving connection requests by spawning a new connection 
 * handling thread for each request received on the bound server socket.
 */
public class WelcomeThread implements Runnable {
    private final boolean DEBUG = false;
    private int PORT = 42015; // using the dnChat protocol's default port.

    private LinkedList<Thread> openConnections = new LinkedList<>();

    public WelcomeThread(int port) {
        this.PORT = port;
    }

    @Override
    public void run() {
        try {
            //First create the server port
            ServerSocket socket = new ServerSocket(PORT);
            if (DEBUG) {
                System.out.println("[TCP] Welcome Socket bound on port " + PORT + ".");
            }

            //Accept all incoming connections on the server socket.
            //socket.accept() blocks hence there is no busy wait
            while (true) {
                Thread conn = new Thread(new ConnectionThread(socket.accept()));
                openConnections.add(conn);
                conn.start();
            }
        } catch (IOException e) {
            for(Thread t : openConnections) {
                t.interrupt();
            }
            e.printStackTrace();
        }
    }
}