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
    public void execute(DNConnection connection, BufferedOutputStream bw, Socket clientSocket) throws IOException {
        if (!this.validPassword) {
            bw.write(FrameFactory.TextFrame(ChatMsgFactory.createResponse("FAIL", this.id, new String[]{"PASSWORD"})));
        } else if(DNChat.getInstance().isNameTaken(name)) {
            bw.write(FrameFactory.TextFrame(ChatMsgFactory.createResponse("FAIL", this.id, new String[]{"NAME"})));
        } else {
            bw.write(FrameFactory.TextFrame(ChatMsgFactory.createResponse("OKAY", this.id, new String[] {})));

            connection.setUserId(this.id);
            connection.setUserName(this.name);

            DNChat.getInstance().addConnection(this.id, connection);

        }
        bw.flush();
    }
}
