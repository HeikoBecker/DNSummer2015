import java.util.Random;

public class Message {
    private static Random rnd = new Random();

    public int Id;
    public String Type;
    public String WebSocketExtensions;
    public String WebSocketKey;
    public String Verb;

    public Message() {
        Id = rnd.nextInt();
    }


    @Override
    public String toString() {
        return Type + " " + Id;
    }
}
