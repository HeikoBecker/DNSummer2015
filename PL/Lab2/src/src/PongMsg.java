import java.io.IOException;


public class PongMsg extends Message {

    @Override
    public void execute(DNConnection connection) throws IOException {
        //PAGE 37, RFC 6455
        //PONG frame is unidirectional heartbeat --> no sending
        //on receipt
    }

    //TODO: Method to notify server that client is awake!
}
