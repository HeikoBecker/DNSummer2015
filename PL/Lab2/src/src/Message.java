import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public abstract class Message {
    public String id;
    public String Type;

    public Message() { }

    @Override
    public String toString() {
        return Type + " " + id;
    }

    public abstract void execute(DNConnection connection, BufferedOutputStream bw, Socket clientSocket) throws IOException;
}
