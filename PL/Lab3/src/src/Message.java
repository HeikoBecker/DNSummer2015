import java.io.IOException;

public abstract class Message {
    public String Id;
    public String Type;

    public Message() { }

    @Override
    public String toString() {
        return Type + " " + Id;
    }

    /*
     * Apply the "effects" of the message to the peer.
     * Can be compared to the Command Pattern.
     */
    public abstract void execute(Peer peer) throws IOException;

}
