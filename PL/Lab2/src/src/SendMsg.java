import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SendMsg extends Message {

    private final String message;
    private final String recipient;
    private final String userId;

    public SendMsg(String id, String userId, String recipient, String message) {
        this.id = id;
        this.userId = userId;
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
        // TODO: put in correct length limitation
        if (this.message.length() >= 30) {
            bw.write(FrameFactory.TextFrame(ChatMsgFactory.createResponse("FAIL", this.id, new String[]{"LENGTH"})));
        } else {
            bw.write(FrameFactory.TextFrame(ChatMsgFactory.createResponse("OKAY", this.id, new String[] {})));
        }
        DNChat.getInstance().sendMessage(this, connection);
        bw.flush();
    }
}
