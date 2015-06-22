import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class AcknMsg extends Message {

    public AcknMsg(String id) {
        this.id = id;
    }

    @Override
    public void execute(DNConnection connection, BufferedOutputStream bw, Socket clientSocket) throws IOException {
        DNChat.getInstance().sendAcknowledgement(this, connection);
    }
}
