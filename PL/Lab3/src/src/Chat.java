import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/*
 * Global Chat Server instance.
 * Aggregates information: Authenticated clients with ID, Messages and outstanding ACKs
 */
public class Chat {
	//Minutes to wait until cleanup occurs, in milliseconds, currently 1 minute
    private static final long CLEANTIME = 1 * 1000;
    //maximal age in milliseconds, currently 5 minutes
    private static final long MAXAGE = 5 * 1000; 
	private static Chat instance = null;

	/*
	 * Cleanup Task removing all messages that are older than MAXAGE minutes.
	 * 
	 * Here we derive from the specification as explained in
	 * https://dcms.cs.uni-saarland.de/dn/forum/viewtopic.php?f=3&t=109
	 */
    private class Cleaner extends TimerTask {

		@Override
		public void run() {
			synchronized (outstandingAcks){
				for (String id : outstandingAcks.keySet() ){
					OutstandingAcknowledgements acks = outstandingAcks.get(id); 
					if (acks.getAge() + MAXAGE < System.currentTimeMillis()){
						outstandingAcks.remove(id);
					}
				}
			}
		}
    	
    }
    
    // Client ID -> Client
    private HashMap<String, Client> clients = new HashMap<>();

    // Message ID -> Collection of outstanding acknowledgements
    private HashMap<String, OutstandingAcknowledgements> outstandingAcks = new HashMap<>();

    private Timer timer;
    
    private Chat() {
    	//Start cleanup timer.
    	timer = new Timer();
    	timer.scheduleAtFixedRate(new Cleaner(), new Date(System.currentTimeMillis()+ Chat.CLEANTIME), Chat.CLEANTIME);
    }

    public static synchronized Chat getInstance() {
        if (instance == null) {
            instance = new Chat();
        }

        return instance;
    }

    // ----------------- COLLISION CHECKS -----------------

    public synchronized boolean isNameTaken(String userName) {
        boolean result = false;
        for (Client existingClient : clients.values()) {
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

    public synchronized boolean isMessageIdOpenForAckn(String messageId, Client otherClient) {
        return outstandingAcks.containsKey(messageId) && outstandingAcks.get(messageId).contains(otherClient.getUserId());
    }

    // ----------------- RELAYING MESSAGES TO OTHER CLIENTS -----------------

    public synchronized void emitMessage(SendChatMsg msg, Client sendingClient) throws IOException {
        String recipientName = msg.getRecipient();
        if (recipientName.equals("*")) {
            for (Client receivingClient : clients.values()) {
                if (!receivingClient.equals(sendingClient)) {
                    receivingClient.emitSendChatMsg(msg, sendingClient.getUserId());
                    storeMessage(msg.getId(), sendingClient.getUserId(), receivingClient.getUserId());
                }
            }
        } else {
            Client receivingClient = clients.get(recipientName);
            receivingClient.emitSendChatMsg(msg, sendingClient.getUserId());
            storeMessage(msg.getId(), sendingClient.getUserId(), receivingClient.getUserId());
        }
    }

    public synchronized void emitAcknowledgement(AcknChatMsg msg, Client sendingClient) throws IOException {
        if (outstandingAcks.containsKey(msg.Id)) {
            String receiverId = sendingClient.getUserId();
            clients.get(outstandingAcks.get(msg.Id).getSenderId()).emitAcknChatMsg(msg, receiverId);
            removeMessage(msg.Id, receiverId);
        }
    }

    /*
     * A message is stored so future acknowledgements can be checked.
     */
    private void storeMessage(String messageId, String senderId, String receiverId) {
        if (!outstandingAcks.containsKey(messageId)) {
            outstandingAcks.put(messageId, new OutstandingAcknowledgements(senderId, System.currentTimeMillis()));
        }
        outstandingAcks.get(messageId).add(receiverId);
    }

    /*
     * Removes outstanding acknowledgement. We comply to the reference implementation, as mentioned in:
     * https://dcms.cs.uni-saarland.de/dn/forum/viewtopic.php?f=3&t=109
     *
     * A message's id can be reused, when all clients have sent their acknowledgement.
     *
     */
    private void removeMessage(String messageId, String receiverId) {
        outstandingAcks.get(messageId).remove(receiverId);
        if (outstandingAcks.get(messageId).size() == 0) {
            outstandingAcks.remove(messageId);
        }
    }


    // ----------------- ENTER / LEAVE -----------------

    /*
     * Add a client and tell all other client that she joined.
     * Inform the newly joined client about others in the room.
     */
    public synchronized void registerClient(Client newClient) throws IOException {
        for (Client existingClient : clients.values()) {
            existingClient.emitArrvChatMsg(newClient);
            newClient.emitArrvChatMsg(existingClient);
        }
        clients.put(newClient.getUserId(), newClient);
    }

    /*
     * Remove the client and tell all other clients that she left.
     */
    public synchronized void unregisterClient(String userId) throws IOException {
        clients.remove(userId);
        for (Client existingClient : clients.values()) {
            existingClient.emitLeftChatMsg(userId);
        }
    }
}