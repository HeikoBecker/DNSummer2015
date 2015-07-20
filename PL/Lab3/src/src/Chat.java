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
    private static final int MAX_HOP_COUNT = 16;
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
            announceChangedForwardingTable(client.getUserId(), client.getHopCount());
        }
    }

    public synchronized void receiveArrv(RemoteArrvChatMsg arrvChatMsg,
                                         Server sendingServer) throws IOException {
        log("Received ARRV broadcast: " + arrvChatMsg.getUserName() + ", " + arrvChatMsg.getDescription() + ", " + arrvChatMsg.getHopCount() + ")");

        int shortestHopCount = findShortestHopCountForClient(arrvChatMsg.getId());

        // Check if this is a local client and ignore if it is.
        if (clients.get(arrvChatMsg.getId()) != null) {
            return;
        } else {
            // This client was already connected, but the hop counted might have changed
            RemoteClient existingClient = sendingServer.getClient(arrvChatMsg.getId());
            if (existingClient != null) {
                if (existingClient.getHopCount() != arrvChatMsg.getHopCount()) {
                    existingClient.setHopCount(arrvChatMsg.getHopCount());
                } else {
                    return; // If the hop count did not change, ignore this message.
                }
            } else {
                sendingServer.registerClient(arrvChatMsg);
            }
        }
        announceChangedForwardingTable(arrvChatMsg.getId(), shortestHopCount);
    }

    public void receiveLeft(RemoteLeftChatMsg remoteLeftChatMsg,
                            Server sendingServer) throws IOException {
        log("Received LEFT broadcast: " + remoteLeftChatMsg.getId());
        int shortestHopCount = findShortestHopCountForClient(remoteLeftChatMsg.getId());

        // Check if this is a local client and ignore if it is.
        if (clients.get(remoteLeftChatMsg.getId()) != null) {
            return;
        } else {
            if (sendingServer.getClient(remoteLeftChatMsg.getId()) != null) {
                sendingServer.unregisterClient(remoteLeftChatMsg.getId());
            }
        }
        announceChangedForwardingTable(remoteLeftChatMsg.getId(), shortestHopCount);
    }

    private int findShortestHopCountForClient(String id) {
        if (clients.get(id) != null) {
            return 0;
        } else {
            Server bestNextHopForClient = findBestNextHopForClient(id);
            if (bestNextHopForClient != null) {
                return bestNextHopForClient.getClient(id).getHopCount();
            }
        }
        return MAX_HOP_COUNT;
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
        LinkedList<String> announcedIds = new LinkedList<>();

        // Forward information about local clients
        for (String id : this.clients.keySet()) {
            announcedIds.push(id);
            LocalClient client = this.clients.get(id);
            server.emitArrv(id, client.getUserName(), "Group 25", 1);
        }


        // Forward information about remote clients
        for (Server remoteServer : federationServers) {
            for (RemoteClient client : remoteServer.getClients()) {
                String remoteClientId = client.getUserId();
                if (!announcedIds.contains(remoteClientId)) {
                    announcedIds.push(remoteClientId);
                    server.emitArrv(remoteClientId, client.getUserName(),
                            client.getDescription(), findShortestHopCountForClient(remoteClientId) + 1);
                }
            }
        }
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
        if (!this.broadcastedMessages.containsKey(msg.getId())) {
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

        LinkedList<String> announcedIds = new LinkedList<>();
        for (Server server : federationServers) {
            server.emitArrv(newClient.getUserId(), newClient.getUserName(),
                    "Group 25", 1);

            for (RemoteClient remoteClient : server.getClients()) {
                if (!announcedIds.contains(remoteClient.getUserId())) {
                    announcedIds.push(remoteClient.getUserId());
                    newClient.emitArrvChatMsg(remoteClient.getUserId(), remoteClient.getUserName(), remoteClient.getDescription());
                }
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

    private void announceChangedForwardingTable(String clientId, int previousHopCount) throws IOException {
        boolean left = false;
        int newShortestHopCount = MAX_HOP_COUNT;
        RemoteClient bestRemoteClient = null;
        Server bestNextHopForClient = findBestNextHopForClient(clientId);
        if (bestNextHopForClient == null) {
            left = previousHopCount < MAX_HOP_COUNT;
        } else {
            bestRemoteClient = bestNextHopForClient.getClient(clientId);
            newShortestHopCount = bestRemoteClient.getHopCount();
        }

        if (left || (newShortestHopCount != previousHopCount && bestRemoteClient != null)) {
            for (LocalClient client : this.clients.values()) {
                if (left) {
                    client.emitLeftChatMsg(clientId);
                } else if (previousHopCount >= MAX_HOP_COUNT) {
                    client.emitArrvChatMsg(clientId, bestRemoteClient.getUserName(), bestRemoteClient.getDescription());
                }
            }

            // Forward to other servers what the new status is.
            for (Server remote : federationServers) {
                if (left) {
                    remote.emitLeft(clientId);
                } else {
                    remote.emitArrv(clientId,
                            bestRemoteClient.getUserName(),
                            bestRemoteClient.getDescription(),
                            newShortestHopCount + 1);
                }
            }
        }
    }

    // ----------------- DEBUGGING -----------------

    protected boolean DEBUG = false;

    protected void log(String msg) {
        if (DEBUG) {
            System.out.println("[CHAT] " + msg);
        }
    }
}