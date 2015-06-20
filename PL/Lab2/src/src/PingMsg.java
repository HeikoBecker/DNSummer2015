import java.io.PrintWriter;
import java.net.Socket;


public class PingMsg extends Message {

	@Override
	public void execute (PrintWriter pr, Socket clientSocket){
		byte[] PongFrame = FrameFactory.PongFrame();
		pr.print(PongFrame);
	}
}
