import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/*
 * @class ConnectionThread
 * A Thread to handle communication on a single TCP connection between a client 
 * and a server.
 */
public class ConnectionThread extends Thread {
    private final boolean DEBUG = true;

    private Peer peer;

    public ConnectionThread(Socket peerSocket) throws IOException {
        if (DEBUG) {
            System.out.println("[TCP] New connection established on "+peerSocket.getInetAddress()+":"+peerSocket.getPort());
        }

        // create a new client and let it execute
        peer = new Peer(peerSocket);
        Message msg = peer.initialize();
        if (msg.getClass() == RemoteSrvrChatMsg.class) {
            peer = new Server(peer);
            Chat.getInstance().addFederationServer((Server) peer);
        } else {
            peer = new LocalClient(peer);
        }
        msg.execute(peer);
    }

    public ConnectionThread(String host, int connectPort) throws IOException, InterruptedException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(host, connectPort), 5000);
        peer = new Server(socket);
        peer.connect(host);
        Chat.getInstance().addFederationServer((Server) peer);
    }

    @Override
    public void run() {
        try {
            peer.run();
            peer.exit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
