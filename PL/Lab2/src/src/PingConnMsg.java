import java.io.IOException;


public class PingConnMsg extends Message {

	@Override
	public void execute(Client client) throws IOException {
		client.emitFrame(FrameFactory.PongFrame());
	}
}
