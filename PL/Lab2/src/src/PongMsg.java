import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;


public class PongMsg extends Message {

	@Override
	public void execute(BufferedOutputStream bw, PrintWriter pr, Socket clientSocket) throws IOException {
		//PAGE 37, RFC 6455
		//PONG frame is unidirectional heartbeat --> no sending
		//on receipt
	}

	//TODO: Method to notify server that client is awake!
}
