import java.io.IOException;


public class HTTPMsg extends Message {
    public String WebSocketExtensions;
    public String WebSocketKey;
    public boolean isCorrectProtocol;

    @Override
    public void execute(DNConnection connection) throws IOException {

    }
}
