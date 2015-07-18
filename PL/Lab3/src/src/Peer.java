import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;

public class Peer {
	// Network Level
	protected WebSocket websocket;

	public Peer() {
		websocket = null;
	}

	public Peer(Socket peerSocket) throws IOException {
		this.websocket = new WebSocket(peerSocket);
	}

	public Message initialize() throws IOException {
		try {
			if (websocket.awaitHandshake()) {
				Message websocketMessage = this.websocket.getWebsocketMessage();
				if (websocketMessage == null) {
					exit();
				}
				return websocketMessage;
			}
		} catch (SocketException | NoSuchAlgorithmException e) {
			System.out.println(e.getMessage());
		} catch (InternalServerException e) {
			log("Internal Server Error. This means something is not correctly typed");
		}
		return new CloseConnMsg();
	}

	public void run() throws IOException {
		while (!websocket.isClosed()) {
			// Let new message execute, resp. send messages on socket
			Message websocketMessage = this.websocket.getWebsocketMessage();
			if (websocketMessage == null) {
				break;
			}
			websocketMessage.execute(this);
		}
	}

	// ----------------- EMIT METHODS TO FORWARD TO SOCKET -----------------
	public void emit(boolean asClient, String command, String id, String[] lines)
			throws IOException {
		this.websocket.emit(asClient, command, id, lines);
	}

	public void emit(boolean asClient, String command, String id)
			throws IOException {
		this.emit(asClient, command, id, new String[] {});
	}

	public void emitFrame(byte[] frame) throws IOException {
		this.websocket.emitFrame(frame);
	}

	// ----------------- ENTER / LEAVE -----------------
	// These methods are abstract as a the separate peer instances/subclasses
	// (LocalClient/Server) must override these methods

	public void authenticate(String userId, String userName) throws IOException, InternalServerException{
		throw new InternalServerException();
	}

	public boolean isAuthenticated() throws InternalServerException{
		throw new InternalServerException();
	}

	// ----------------- RECEIVE METHODS -----------------
	// Receiving methods must be abstract too. Otherwise a peer subclass could
	// use this method which is not intended

	public void recvAcknChatMsg(LocalAcknChatMsg acknMsg) throws IOException, InternalServerException{
		throw new InternalServerException();
	}

	public void recvSendChatMsg(LocalSendChatMsg sendMsg) throws IOException, InternalServerException {
		throw new InternalServerException();
	}

	public void connect(String host) throws IOException, InternalServerException{
		throw new InternalServerException();
	}

	public void exit() throws InternalServerException {
		throw new InternalServerException();
	}

	// ----------------- DEBUGGING -----------------
	protected final boolean DEBUG = false;

	protected void log(String msg) {
		if (DEBUG) {
			System.out.println("[P] " + msg);
		}
	}
}
