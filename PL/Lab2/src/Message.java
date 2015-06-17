import java.util.Random;

public class Message {
    private static Random rnd = new Random();

    public int Id;
    public String Type;

    public Message() {
        Id = rnd.nextInt();
    }

    public String WebSocketKey;

    @Override
    public String toString() {
        return Type + " " + Id;
    }
}
