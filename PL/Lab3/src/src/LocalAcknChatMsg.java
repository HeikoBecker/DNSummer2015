import java.io.IOException;

/*
 *  High Level encapsulation of a single ACKN message.
 */
public class LocalAcknChatMsg extends Message {

    public LocalAcknChatMsg(String id) {
        this.type = "dnChat-ACKN";
        this.id = id;
    }

    @Override
    public void execute(Peer peer) throws IOException {
        if (!peer.isAuthenticated()) {
            peer.emit(false, "INVD", "0");
            peer.exit();
        } else if (!Chat.getInstance().isMessageIdOpenForAckn(this.id, (LocalClient) peer)) {
            peer.emit(false, "FAIL", this.id, new String[]{"NUMBER"});
        } else {
            peer.recvAcknChatMsg(this);
        }
    }
}
