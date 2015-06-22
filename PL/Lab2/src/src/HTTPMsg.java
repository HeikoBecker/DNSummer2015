import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;


public class HTTPMsg extends Message {
    public String WebSocketExtensions;
    public String WebSocketKey;
    public String Verb;
    public boolean isCorrectProtocol;

    @Override
    public void execute(DNConnection connection, BufferedOutputStream bw, Socket clientSocket) throws IOException {

    }
}
