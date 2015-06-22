import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;

public abstract class Message {
    private static Random rnd = new Random();

    public Long Id;
    public String Type;

    public Message() {
        this.Id = rnd.nextLong();
    }

    @Override
    public String toString() {
        return Type + " " + Id;
    }

    public abstract void execute(BufferedOutputStream bw, PrintWriter pr, Socket clientSocket) throws IOException;
}
