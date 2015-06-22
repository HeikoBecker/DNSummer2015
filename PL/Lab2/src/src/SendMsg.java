import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SendMsg extends Message {

    private final String message;
    private final String recipient;

    public SendMsg(String id, String recipient, String message) {
        this.id = id;
        this.recipient = recipient;
        this.message = message;
    }

    @Override
    public void execute(BufferedOutputStream bw, Socket clientSocket) throws IOException {
        if (this.message.length() >= 30) {
            bw.write(FrameFactory.TextFrame(ChatMsgFactory.createResponse("FAIL", this.id, new String[]{"LENGTH"})));
        } else {
            bw.write(FrameFactory.TextFrame(ChatMsgFactory.createResponse("OKAY", this.id, new String[] {})));
        }
        bw.flush();
    }
}
