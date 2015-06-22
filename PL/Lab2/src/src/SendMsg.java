import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SendMsg extends Message {
    public SendMsg(String recipient, String message) {
    }

    @Override
    public void execute(BufferedOutputStream bw, Socket clientSocket) throws IOException {

    }
}
