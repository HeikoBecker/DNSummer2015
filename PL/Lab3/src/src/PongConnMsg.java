import java.io.IOException;


public class PongConnMsg extends Message {

    public PongConnMsg() {
        this.Type = "ws-PONG";
    }

    @Override
    public void execute(Peer peer) throws IOException {
        //PAGE 37, RFC 6455
        //PONG frame is unidirectional heartbeat --> no sending
        //on receipt
    }
}
