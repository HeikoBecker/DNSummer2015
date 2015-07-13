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
	public synchronized void addFederationServer(Server server) throws IOException {
		federationServers.add(server);
	}

	public synchronized void receiveArrvBroadcast(ArrvChatMsg arrvChatMsg,
			Server sendingServer) throws IOException {
		// Check if incoming message has been broadcast already
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
				remote.sendArrv(arrvChatMsg);
			}
		}
		broadcastedMessages.put(arrvChatMsg.getId(),
                new Date(System.currentTimeMillis()));
	}

    public void receiveAcknBroadcast(RemoteAcknChatMsg acknChatMsg, Server sendingServer) throws IOException {
        log("Received ACKN broadcast");
        for (LocalClient localClient : this.clients.values()) {
            if(acknChatMsg.getSenderUserId().equals(localClient.getUserId())) {
                localClient.emitAcknChatMsg(acknChatMsg.getId(), acknChatMsg.getAcknUserId());
            }
        }
        for (Server remote : federationServers) {
            if (!remote.equals(sendingServer)) {
                remote.sendAckn(acknChatMsg);
            }
        }
        broadcastedMessages.put(acknChatMsg.getId(),
                new Date(System.currentTimeMillis()));
    }


    /*
	 * Synchronized sending of all registered users. Must be synchronized as we
	 * have no concurrent hashmap
	 */
	public synchronized void advertiseCurrentUsers(Server server)
			throws IOException {
		log("Advertising own registered users to new server");

		// Forward information about local clients
		for (String id : this.clients.keySet()) {
			LocalClient client = this.clients.get(id);
			server.sendArrv(id, client.getUserName(), "Group 12",
					client.getHopCount() + 1);
		}

		// Forward information about remote clients
		for (Server remoteServer : federationServers) {
			for (RemoteClient client : remoteServer.getClients()) {
				server.sendArrv(client.getUserId(), client.getUserName(), "",
						client.getHopCount() + 1);
			}
		}
	}

	/*
	 * @return True only if we can find a message corresponding to the given
	 * @param ID, which means that we already saw this message
	 */
	public synchronized boolean broadcasted(String id) {
		return (this.broadcastedMessages.containsKey(id));
	}

	// ----------------- COLLISION CHECKS -----------------

	public synchronized boolean isNameTaken(String userName) {
		boolean result = false;
		for (LocalClient existingClient : clients.values()) {
			if (existingClient.getUserName().equals(userName)) {
				result = true;
				break;
			}
		}
		return result;
	}

	public synchronized boolean isUserIdTaken(String userId) {
		return clients.containsKey(userId);
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

	public synchronized void emitMessage(SendChatMsg msg,
			String senderId) throws IOException {
		String recipientName = msg.getRecipient();
        if (recipientName.equals("*")) {
			for (LocalClient receivingClient : clients.values()) {
				if (!receivingClient.getUserId().equals(senderId)
                        && receivingClient.getHopCount() == 0) {
					receivingClient.emitSendChatMsg(msg, senderId);
					storeMessage(msg.getId(), senderId,
							receivingClient.getUserId());
				}
			}
		} else {
			LocalClient receivingClient = clients.get(recipientName);
			receivingClient.emitSendChatMsg(msg, senderId);
			storeMessage(msg.getId(), senderId, receivingClient.getUserId());
		}

		for (Server remoteServer : this.federationServers){
			//TODO: Format compliance?
			remoteServer.emitMessage(msg, senderId);
		}
		broadcastedMessages.put(msg.getId(), new Date(System.currentTimeMillis()));
	}

	public synchronized void emitAcknowledgement(AcknChatMsg msg,
			LocalClient sendingClient) throws IOException {
		if (outstandingAcks.containsKey(msg.id)) {
			String acknUserId = sendingClient.getUserId();
            String senderId = outstandingAcks.get(msg.id).getSenderId();
            LocalClient localClient = clients.get(senderId);
            if(localClient != null) {
                localClient.emitAcknChatMsg(msg, acknUserId);
            } else {
                String messageIdentifier = msg.getId() + '-' + acknUserId;
                if(!broadcasted(messageIdentifier)) {
                    // TODO: Actively look for the optimal server to send the ackn to.
                    for (Server remoteServer : this.federationServers){
                        remoteServer.emitAcknowledgement(msg, acknUserId, senderId);
                    }
                    broadcastedMessages.put(messageIdentifier, new Date(System.currentTimeMillis()));
                }
            }
            removeMessage(msg.id, acknUserId);
		}
	}

	/*
	 * A message is stored so future acknowledgements can be checked.
	 */
	private void storeMessage(String messageId, String senderId,
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
	private void removeMessage(String messageId, String receiverId) {
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
			server.sendArrv(newClient.getUserId(), newClient.getUserName(),
					"Group 12", 0);

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
			server.sendLeft(userId);
		}
	}

	protected boolean DEBUG = true;

	protected void log(String msg) {
		if (DEBUG) {
			System.out.println("[CHAT] " + msg);
		}
	}
}