import java.io.PrintWriter;
import java.net.Socket;


public class PongMsg extends Message {

	@Override
	public void execute (PrintWriter pr, Socket clientSocket){
		//PAGE 37, RFC 6455
		//PONG frame is unidirectional heartbeat --> no sending
		//on receipt
	}
	
	//TODO: Method to notify server that client is awake!
}
