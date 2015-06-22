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

    public String getRecipient() {
        return recipient;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public void execute(DNConnection connection, BufferedOutputStream bw, Socket clientSocket) throws IOException {
        /*
         * Note: The length limitation to 384 bytes is derived by reversing the reference implementation.
         */

        if (this.message.length() > 384) {
            bw.write(FrameFactory.TextFrame(ChatMsgFactory.createResponse("FAIL", this.id, new String[]{"LENGTH"})));
        } else {
            bw.write(FrameFactory.TextFrame(ChatMsgFactory.createResponse("OKAY", this.id, new String[]{})));
            DNChat.getInstance().sendMessage(this, connection);
        }
        bw.flush();
    }
}
