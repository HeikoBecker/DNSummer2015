public class Server extends Peer {
    public Server(Peer peer) {
        this.websocket = peer.websocket;
    }
}
