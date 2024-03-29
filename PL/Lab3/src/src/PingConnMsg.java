import java.io.IOException;


public class PingConnMsg extends Message {

	public PingConnMsg() {
		this.type = "ws-PING";
	}

	@Override
	public void execute(Peer peer) throws IOException {
		peer.emitFrame(FrameFactory.PongFrame());
	}
}
