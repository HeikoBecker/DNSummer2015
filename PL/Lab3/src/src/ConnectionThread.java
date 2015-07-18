import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/*
 * @class ConnectionThread
 * A Thread to handle communication on a single TCP connection between a client 
 * and a server.
 */
public class ConnectionThread implements Runnable {
    private final boolean DEBUG = false;

    private Peer peer;

    public ConnectionThread(Socket peerSocket) throws IOException {
        if (DEBUG) {
            System.out.println("[TCP] New connection established on "+peerSocket.getInetAddress()+":"+peerSocket.getPort());
        }

        // create a new client and let it execute
        //TODO Clean the code logic up here
        peer = new Peer(peerSocket);
        Message msg = peer.initialize();
        if (msg.getClass() == RemoteSrvrChatMsg.class) {
            peer = new Server(peer);
            Chat.getInstance().addFederationServer((Server) peer);
        } else {
            peer = new LocalClient(peer);
        }
        //First execute the message
        msg.execute(peer);
    }

    public ConnectionThread(String host, int connectPort) throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, connectPort), 5000);
            Server server = new Server(socket);
            peer = server;
            peer.websocket.setWsClient();
            peer.connect(host);
            if (!server.isFailed())
            	Chat.getInstance().addFederationServer((Server) peer);
            //in the else case we would have to close the connection, but the connect method took care of this
            //therefore "run" on the peer will terminate
            //TODO: Notify server user somehow
        } catch (UnknownHostException e) {
            System.out.println("Failed to establish connection with " + host + ":" + connectPort + ". Unknown host.");
            throw new IOException();
        } catch (IOException e) {
            System.out.println("Failed to establish connection with " + host + ":" + connectPort);
            throw e;
        } catch (InternalServerException e) {
			System.out.println("Internal Server Error. Something went wrong with the internal typing.");
		}
    }

    @Override
    public void run() {
        try {
        	peer.run();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
				peer.exit();
			} catch (InternalServerException e) {
				System.out.println("Internal Server Error. Something went wrong with the internal typing.");
			}
        }
    }
}
