import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class AuthMsg extends Message {
    private static final String groupPassword = "3YnnafwB";

    private boolean validPassword = false;
    private String name;

    public AuthMsg(Long id, String name, String password) {
        this.Id = id;
        this.name = name;

        if (password.equals(groupPassword)) {
            this.validPassword = true;
        }
    }

    @Override
    public void execute(BufferedOutputStream bw, Socket clientSocket) throws IOException {
        if (!this.validPassword) {
            bw.write(FrameFactory.TextFrame(ChatMsgFactory.createResponse("FAIL", this.Id, new String[]{"PASSWORD"})));
        } else {
            bw.write(FrameFactory.TextFrame(ChatMsgFactory.createResponse("OKAY", this.Id, new String[] {})));
        }
        bw.flush();
    }
}
