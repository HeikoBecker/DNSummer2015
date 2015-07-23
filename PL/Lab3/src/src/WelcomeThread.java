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
    private LinkedList<ConnectionThread> openConnectionThreads = new LinkedList<>();
    private ServerSocket socket;

    public WelcomeThread(int port) {
        this.PORT = port;
    }

    @Override
    public void run() {
        try {
            //First create the server port
            socket = new ServerSocket(PORT);
            if (DEBUG) {
                System.out.println("[TCP] Welcome Socket bound on port " + PORT + ".");
            }

            //Accept all incoming connections on the server socket.
            //socket.accept() blocks hence there is no busy wait
            while (true) {
                ConnectionThread ct = new ConnectionThread(socket.accept());
                Thread t = new Thread(ct);
                openConnections.add(t);
                openConnectionThreads.add(ct);
                t.start();
            }
        } catch (IOException e) {
            for(ConnectionThread t: openConnectionThreads) {
                try {
                    t.exit();
                } catch (InternalServerException e1) {
                    e1.printStackTrace();
                }
            }
            for(Thread t : openConnections) {
                t.interrupt();
            }
        }
    }

    public void exit() {
        if(socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("COULD NOT CLOSE");
            }
        }
    }
}