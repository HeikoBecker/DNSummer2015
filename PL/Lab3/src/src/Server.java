import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.net.Socket;

public class Server extends Peer {
    public Server(Peer peer) {
        this.websocket = peer.websocket;
    }

    public Server(Socket socket) throws IOException {
        super(socket);
    }

    public void connect(String host) throws IOException, InterruptedException {
        this.websocket.executeHandshake(host);
        this.emit(true, "SRVR", "0");
    }

    @Override
    public void exit() throws IOException {
        System.out.println("SERVER EXITING");
        throw new NotImplementedException();
    }
}
