import java.io.IOException;


public class PingMsg extends Message {

	@Override
	public void execute(DNConnection connection) throws IOException {
		connection.sendFrame(FrameFactory.PongFrame());
	}
}
