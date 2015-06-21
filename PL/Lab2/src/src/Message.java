import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;

public class Message {
    private static Random rnd = new Random();

    public int Id;
    public String Type;
    public String WebSocketExtensions;
    public String WebSocketKey;
    public String Verb;

    public Message() {
        Id = rnd.nextInt();
    }


    @Override
    public String toString() {
        return Type + " " + Id;
    }

//TODO: Make this abstract and have specialized msgs!
	public void execute(BufferedOutputStream bw, PrintWriter pr, Socket clientSocket) throws IOException {
		return;
	}
}
