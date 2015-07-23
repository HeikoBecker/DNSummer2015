import java.io.IOException;

public class RemoteSrvrChatMsg extends Message {

    public RemoteSrvrChatMsg() {
        this.type = "dnChat-SRVR";
    }

    @Override
    public void execute(Peer peer) throws IOException {
        Server server = (Server) peer;
        if(!server.hasAdvertisedUsers()) {
            Chat.getInstance().advertiseCurrentUsers(server);
        } else {
            System.out.println("Received multiple SRVR messages.");
            peer.emit(false, "INVD", "0");
        }
    }
}
