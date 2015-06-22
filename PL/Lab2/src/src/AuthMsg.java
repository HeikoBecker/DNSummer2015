import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class AuthMsg extends Message {
    private static final String groupPassword = "3YnnafwB";

    private boolean validPassword = false;
    private String name;

    public AuthMsg(String id, String name, String password) {
        this.id = id;
        this.name = name;

        if (password.equals(groupPassword)) {
            this.validPassword = true;
        }
    }

    @Override
    public void execute(BufferedOutputStream bw, Socket clientSocket) throws IOException {
        // TODO: check for untaken username

        if (!this.validPassword) {
            bw.write(FrameFactory.TextFrame(ChatMsgFactory.createResponse("FAIL", this.id, new String[]{"PASSWORD"})));
        } else {
            bw.write(FrameFactory.TextFrame(ChatMsgFactory.createResponse("OKAY", this.id, new String[] {})));
        }
        bw.flush();
    }
}
