import java.io.IOException;
import java.net.ServerSocket;

/*
 * @class WelcomeThread
 * Welcomes new arriving connection requests by spawning a new connection 
 * handling thread for each request received on the bound server socket.
 */
public class WelcomeThread implements Runnable {
    private final boolean DEBUG = false;
    private int PORT = 42015; // using the dnChat protocol's default port.

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
                ConnectionThread conn = new ConnectionThread(socket.accept());
                conn.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}