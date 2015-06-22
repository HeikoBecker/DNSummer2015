import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;


public class PingMsg extends Message {

	@Override
	public void execute(DNConnection connection, BufferedOutputStream bw, Socket clientSocket) throws IOException {
		bw.write(FrameFactory.PongFrame());
		bw.flush();
	}
}
