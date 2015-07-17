import java.io.IOException;
import java.util.*;

/*
 * Global Chat Server instance.
 * Aggregates information: Authenticated clients with ID, Messages and outstanding ACKs
 */
public class Chat {
	// Minutes to wait until cleanup occurs, in milliseconds, currently 1 minute
	private static final long CLEANTIME = 1 * 1000;
	// maximal age in milliseconds, currently 5 minutes
	private static final long MAXAGE = 5 * 1000;
	private static Chat instance = null;
	public static final int DEFAULT_PORT = 42015;

	/*
	 * Cleanup Task removing all messages that are older than MAXAGE minutes.
	 * 
	 * Here we derive from the specification as explained in
	 * https://dcms.cs.uni-saarland.de/dn/forum/viewtopic.php?f=3&t=109
	 */
	private class Cleaner extends TimerTask {

		@Override
		public void run() {
			synchronized (outstandingAcks) {
				for (String id : outstandingAcks.keySet()) {
					OutstandingAcknowledgements acks = outstandingAcks.get(id);
					if (acks.getAge() + MAXAGE < System.currentTimeMillis()) {
						outstandingAcks.remove(id);
					}
				}
			}
		}

	}

	// Directly Connected Clients
	// Client ID -> Client
	private HashMap<String, LocalClient> clients = new HashMap<>();

	// Message ID -> Collection of outstanding acknowledgements
	private HashMap<String, OutstandingAcknowledgements> outstandingAcks = new HashMap<>();

	private LinkedList<Server> federationServers = new LinkedList<>();

	// TODO: messages have to be removed again, as e.g. ARRV message have
	// identical sequence numbers
	// Message ID -> Date of adding
	private HashMap<String, Date> broadcastedMessages = new HashMap<>();

	private Timer timer;

	private Chat() {
		// Start cleanup timer.
		timer = new Timer();
		timer.scheduleAtFixedRate(new Cleaner(),
				new Date(System.currentTimeMillis() + Chat.CLEANTIME),
				Chat.CLEANTIME);
	}

	public static synchronized Chat getInstance() {
		if (instance == null) {
			instance = new Chat();
		}

		return instance;
	}

	// ----------------- FEDERATION -----------------
	public synchronized void addFederationServer(Server server)
			throws IOException {
		federationServers.add(server);
	}

	public synchronized void removeFederationServer(Server server)
			throws IOException {
		federationServers.remove(server);
		for (RemoteClient client : server.getClients()) {
			// TODO: think about advertising users only to those servers that we
			// do not use for routing to them. POISONING
			for (Server federationServer : federationServers) {
				try {
					federationServer.emitLeft(client.getUserId());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			for (LocalClient localClient : clients.values()) {
				try {
					localClient.emitLeftChatMsg(client.getUserId());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			// check whether there is still a connection to this user via
			// another server. Then we should send new arrivals in case these
			// users are still present
			// TODO: I abuse the isUserIdTaken method for checking wether the
			// client is still
			// reachable, maybe make this a separate method or so
			if (this.isUserIdTaken(client.getUserId())) {
				reannounce(client);
			}
		}
	}

	public synchronized void receiveArrv(RemoteArrvChatMsg arrvChatMsg,
			Server sendingServer) throws IOException {
		// Check if incoming message has been broadcast already
		if (!broadcasted(arrvChatMsg.id + "-ARRV")) {
			log("Received ARRV broadcast");

			// Forward to local clients
			for (LocalClient client : this.clients.values()) {
				if (!arrvChatMsg.getId().equals(client.getUserId())) {
					client.emitArrvChatMsg(arrvChatMsg.getId(),
							arrvChatMsg.getUserName());
				}
			}

			// Forward to other servers
			for (Server remote : federationServers) {
				if (!remote.equals(sendingServer)) {
					remote.emitArrv(arrvChatMsg.getId(),
							arrvChatMsg.getUserName(),
							arrvChatMsg.getDescription(),
							((arrvChatMsg.getHopCount() + 1)));
				}
			}
			broadcastedMessages.put(arrvChatMsg.getId() + "-ARRV", new Date(
					System.currentTimeMillis()));

			// the RemoteClient should be added to the sending Server no matter
			// how far away it is
			// otherwise we are not able to tell the presence of this user to
			// local clients anymore
			// or increases in distance due to appearing/disappearing
			// connections in the network
			sendingServer.registerClient(arrvChatMsg);
		}
	}

	public void receiveLeft(RemoteLeftChatMsg remoteLeftChatMsg,
			Server sendingServer) throws IOException {
		if (!broadcasted(remoteLeftChatMsg.getId() + "-LEFT")) {
			log("Received LEFT broadcast");
			String leftUserId = remoteLeftChatMsg.getId();
			for (LocalClient client : this.clients.values()) {
				client.emitLeftChatMsg(leftUserId);
			}

			for (Server remote : federationServers) {
				if (!remote.equals(sendingServer)) {
					remote.emitLeft(leftUserId);
				}
			}

			sendingServer.unregisterClient(remoteLeftChatMsg.id);
			broadcastedMessages.put(remoteLeftChatMsg.getId() + "-LEFT",
					new Date(System.currentTimeMillis()));

			// Check wether the user is still reachable, as the remote server
			// may have lost connection to another server
			// code copied from removeFederationServer method.
			// Maybe refactor into one method!
			// check whether there is still a connection to this user via
			// another server. Then we should send new arrivals in case these
			// users are still present
			// TODO: I abuse the isUserIdTaken method for checking wether the
			// client is still
			// reachable, maybe make this a separate method or so
			if (this.isUserIdTaken(remoteLeftChatMsg.getId())) {
				this.reannounce(remoteLeftChatMsg);
			}
		}
	}

	public synchronized void receiveAckn(RemoteAcknChatMsg acknChatMsg)
			throws IOException {
		log("Received ACKN");
		LocalClient localClient = clients.get(acknChatMsg.getSenderUserId());
		if (localClient != null) {
			localClient.emitAcknChatMsg(acknChatMsg.getId(),
					acknChatMsg.getAcknUserId());
		} else {
			Server remote = findBestNextHopForClient(acknChatMsg
					.getSenderUserId());
			remote.emitAckn(acknChatMsg);
		}
	}

	/*
	 * Synchronized sending of all registered users. Must be synchronized as we
	 * have no concurrent hashmap.
	 */
	public synchronized void advertiseCurrentUsers(Server server)
			throws IOException {
		log("Advertising own registered users to new server");

		// Forward information about local clients
		for (String id : this.clients.keySet()) {
			LocalClient client = this.clients.get(id);
			server.emitArrv(id, client.getUserName(), "Group 25", 1);
		}

		// Forward information about remote clients
		for (Server remoteServer : federationServers) {
			for (RemoteClient client : remoteServer.getClients()) {
				server.emitArrv(client.getUserId(), client.getUserName(),
						client.getDescription(), client.getHopCount() + 1);
			}
		}
	}

	/*
	 * @return True only if we can find a message corresponding to the given
	 * 
	 * @param ID, which means that we already saw this message
	 */
	public synchronized boolean broadcasted(String id) {
		return (this.broadcastedMessages.containsKey(id));
	}

	/*
	 * This is basically our routing function that finds the best next hop to
	 * reach client with a given id.
	 */
	public synchronized Server findBestNextHopForClient(String clientId) {
		Server bestNextHop = null;
		int bestHopDistance = 0;
		for (Server remoteServer : federationServers) {
			RemoteClient remoteClient = remoteServer.getClient(clientId);
			if (remoteClient != null) {
				if (bestNextHop == null
						|| bestHopDistance > remoteClient.getHopCount()) {
					bestNextHop = remoteServer;
					bestHopDistance = remoteClient.getHopCount();
				}
			}
		}
		return bestNextHop;
	}

	// ----------------- COLLISION CHECKS -----------------

	public synchronized boolean isNameTaken(String userName) {
		// check local clients
		boolean result = false;
		for (LocalClient existingClient : clients.values()) {
			if (existingClient.getUserName().equals(userName)) {
				result = true;
				break;
			}
		}
		if (result)
			return true;
		// check remote clients
		for (Server server : this.federationServers) {
			for (RemoteClient client : server.getClients()) {
				if (client.getUserName().equals(userName))
					result = true;
				break;
			}
			if (result)
				break;
		}
		return result;
	}

	public synchronized boolean isUserIdTaken(String userId) {
		// check local clients first for complexity reasons
		boolean local = clients.containsKey(userId);
		if (local)
			return true;
		// check remote clients
		for (Server server : this.federationServers) {
			if (server.getClient(userId) != null) {
				return true;
			}
		}
		return false;
	}

	public synchronized boolean isMessageIdTaken(String messageId) {
		return outstandingAcks.containsKey(messageId);
	}

	public synchronized boolean isMessageIdOpenForAckn(String messageId,
			LocalClient otherClient) {
		return outstandingAcks.containsKey(messageId)
				&& outstandingAcks.get(messageId).contains(
						otherClient.getUserId());
	}

	// ----------------- RELAYING MESSAGES TO OTHER CLIENTS -----------------

	public synchronized void emitMessage(LocalSendChatMsg msg, String senderId)
			throws IOException {
		String recipient = msg.getRecipient();

		if (recipient.equals("*")) {
			for (LocalClient receivingClient : clients.values()) {
				if (!receivingClient.getUserId().equals(senderId)) {
					receivingClient.emitSendChatMsg(msg, senderId);
					storeMessage(msg.getId(), senderId,
							receivingClient.getUserId());
				}
			}
			// In this case, the message is broadcasted to all other servers.
			for (Server remoteServer : this.federationServers) {
				remoteServer.emitSend(msg, senderId);
			}
		} else {
			LocalClient receivingClient = clients.get(recipient);
			if (receivingClient != null) {
				receivingClient.emitSendChatMsg(msg, senderId);
				storeMessage(msg.getId(), senderId, receivingClient.getUserId());
			} else {
				Server remote = findBestNextHopForClient(recipient);
				remote.emitSend(msg, senderId);
			}
		}
		broadcastedMessages.put(msg.getId(),
				new Date(System.currentTimeMillis()));
	}

	public synchronized void emitAcknowledgement(LocalAcknChatMsg msg,
			LocalClient sendingClient) throws IOException {
		if (outstandingAcks.containsKey(msg.id)) {
			String acknUserId = sendingClient.getUserId();
			String senderId = outstandingAcks.get(msg.id).getSenderId();
			LocalClient localClient = clients.get(senderId);
			if (localClient != null) {
				localClient.emitAcknChatMsg(msg, acknUserId);
			} else {
				Server remoteServer = findBestNextHopForClient(senderId);
				remoteServer.emitAckn(msg, acknUserId, senderId);
			}
			removeMessage(msg.id, acknUserId);
		}
	}

	/*
	 * A message is stored so future acknowledgements can be checked.
	 */
	private synchronized void storeMessage(String messageId, String senderId,
			String receiverId) {
		if (!outstandingAcks.containsKey(messageId)) {
			outstandingAcks.put(messageId, new OutstandingAcknowledgements(
					senderId, System.currentTimeMillis()));
		}
		outstandingAcks.get(messageId).add(receiverId);
	}

	/*
	 * Removes outstanding acknowledgement. We comply to the reference
	 * implementation, as mentioned in:
	 * https://dcms.cs.uni-saarland.de/dn/forum/viewtopic.php?f=3&t=109
	 * 
	 * A message's id can be reused, when all clients have sent their
	 * acknowledgement.
	 */
	private synchronized void removeMessage(String messageId, String receiverId) {
		outstandingAcks.get(messageId).remove(receiverId);
		if (outstandingAcks.get(messageId).size() == 0) {
			outstandingAcks.remove(messageId);
		}
	}

	// ----------------- ENTER / LEAVE -----------------

	/*
	 * Add a local client and tell all other client that she joined. Inform the
	 * newly joined client about others in the room.
	 */
	public synchronized void registerClient(LocalClient newClient)
			throws IOException {
		log("Register Client: " + newClient.getUserId() + " "
				+ newClient.getUserName());
		for (LocalClient existingClient : clients.values()) {
			existingClient.emitArrvChatMsg(newClient);
			newClient.emitArrvChatMsg(existingClient);
		}

		for (Server server : federationServers) {
			server.emitArrv(newClient.getUserId(), newClient.getUserName(),
					"Group 25", 1);

			for (RemoteClient remoteClient : server.getClients()) {
				newClient.emitArrvChatMsg(remoteClient.getUserId(),
						remoteClient.getUserName());
			}
		}

		clients.put(newClient.getUserId(), newClient);
	}

	/*
	 * Remove the local client and tell all other clients that she left.
	 */
	public synchronized void unregisterClient(String userId) throws IOException {
		clients.remove(userId);
		for (LocalClient existingClient : clients.values()) {
			existingClient.emitLeftChatMsg(userId);
		}

		for (Server server : federationServers) {
			server.emitLeft(userId);
		}
	}

	//TODO: Merge these two methods
	private void reannounce(RemoteClient client) throws IOException {
		Server nextBestRoute = this
				.findBestNextHopForClient(client.getUserId());
		if (nextBestRoute != null) {
			RemoteClient sameUser = nextBestRoute.getClient(client.getUserId());
			RemoteArrvChatMsg newHopCount = new RemoteArrvChatMsg(
					sameUser.getUserId(), sameUser.getUserName(),
					sameUser.getDescription(), sameUser.getHopCount() + 1);
			// TODO: emit new Arrive?
			this.receiveArrv(newHopCount, nextBestRoute);
		} else {
			// TODO: Can this case actually happen?
			for (LocalClient localClient : this.clients.values()) {
				if (localClient.getUserId().equals(client.getUserId())) {

				}
			}
		}
	}
	
	private void reannounce(RemoteLeftChatMsg msg) throws IOException {
		Server nextBestRoute = this
				.findBestNextHopForClient(msg.getId());
		if (nextBestRoute != null) {
			RemoteClient sameUser = nextBestRoute.getClient(msg.getId());
			RemoteArrvChatMsg newHopCount = new RemoteArrvChatMsg(
					sameUser.getUserId(), sameUser.getUserName(),
					sameUser.getDescription(), sameUser.getHopCount() + 1);
			// TODO: emit new Arrive?
			this.receiveArrv(newHopCount, nextBestRoute);
		} else {
			// TODO: Can this case actually happen?
			for (LocalClient localClient : this.clients.values()) {
				if (localClient.getUserId().equals(msg.getId())) {

				}
			}
		}
	}

	protected boolean DEBUG = true;

	protected void log(String msg) {
		if (DEBUG) {
			System.out.println("[CHAT] " + msg);
		}
	}
}