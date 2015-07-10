import java.io.IOException;

/*
 *  High Level encapsulation of a single ACKN message.
 */
public class AcknChatMsg extends Message {

    public AcknChatMsg(String id) {
        this.Type = "dnChat-ACKN";
        this.Id = id;
    }

    @Override
    public void execute(Peer peer) throws IOException {
        if (!peer.isAuthenticated()) {
            peer.emit(false, "INVD", "0");
            peer.exit();
        } else if (!Chat.getInstance().isMessageIdOpenForAckn(this.Id, (Client) peer)) {
            peer.emit(false, "FAIL", this.Id, new String[]{"NUMBER"});
        } else {
            peer.recvAcknChatMsg(this);
        }
    }
}
