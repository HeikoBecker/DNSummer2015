import java.io.IOException;
import java.net.Socket;

/*
 * @class ConnectionThread
 * A Thread to handle communication on a single TCP connection between a client 
 * and a server.
 */
public class ConnectionThread extends Thread {
    private final boolean DEBUG = false;

    Peer peer;
    final Socket peerSocket;

    public ConnectionThread(Socket peerSocket) {
        if (DEBUG) {
            System.out.println("[TCP] New connection established");
        }
        this.peerSocket = peerSocket;
    }

    @Override
    public void run() {
        try {
            // create a new client and let it execute
            peer = new Peer(peerSocket);
            Message msg = peer.initialize();
            if(msg.getClass() == SrvrChatMsg.class) {
                peer = new Server(peer);
            } else {
                peer = new Client(peer);
            }
            System.out.println(msg);
            msg.execute(peer);
            peer.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
