import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;


public class HTTPMsg extends Message {
    public String WebSocketExtensions;
    public String WebSocketKey;
    public String Verb;
    public boolean isCorrectProtocol;

    @Override
    public void execute(BufferedOutputStream bw, Socket clientSocket) throws IOException {

    }
}
