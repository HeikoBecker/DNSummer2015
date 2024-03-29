import java.io.IOException;

public abstract class Message {
    protected String id;
    protected String type;

    public Message() { }

    @Override
    public String toString() {
        return type + " " + id;
    }

    /*
     * Apply the "effects" of the message to the peer.
     * Can be compared to the Command Pattern.
     */
    public abstract void execute(Peer peer) throws IOException;

    public String getId() {
        return id;
    }
}
