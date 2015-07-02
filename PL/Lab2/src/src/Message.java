import java.io.IOException;

public abstract class Message {
    public String id;
    public String Type;

    public Message() { }

    @Override
    public String toString() {
        return Type + " " + id;
    }

    /*
     * Apply the "effects" of the message to the client.
     * Can be compared to the Command Pattern.
     */
    public abstract void execute(Client client) throws IOException;

}
