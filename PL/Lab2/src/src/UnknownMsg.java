import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class UnknownMsg extends Message {
    @Override
    public void execute(BufferedOutputStream bw, Socket clientSocket) throws IOException {

    }
}
