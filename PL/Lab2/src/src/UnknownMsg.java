import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class UnknownMsg extends Message {
    @Override
    public void execute(BufferedOutputStream bw, PrintWriter pr, Socket clientSocket) throws IOException {

    }
}
