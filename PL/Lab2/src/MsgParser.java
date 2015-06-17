import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MsgParser {
    private static final String HOST = "Host";
    private static final String USRAGENT = "User-Agent";
    private static final String ACC = "Accept";
    private static final String ACCLANG = "Accept-Language";
    private static final String ACCENC = "Accept-Encoding";
    private static final String COOKIE = "Cookie";
    private static final String DNT = "DNT";
    private static final String SECWS = "Sec-WebSocket-Version";
    private static final String ORIGIN = "Origin";
    private static final String SECWSEXT = "Sec-WebSocket-Extensions";
    private static final String SECWSKEY = "Sec-WebSocket-Key";
    private static final String CONN = "Connection";
    private static final String PRAGMA = "Pragma";
    private static final String CC = "Cache-Control";
    private static final String UPG = "Upgrade";
    private static final String EMPTY = "";
    private static final String SPLIT = ": ";

    private BufferedReader inputBuffer;

    public MsgParser(InputStream is) {
        this.inputBuffer = new BufferedReader(new InputStreamReader(new DataInputStream(is)));
    }

    public Message getMsg() throws IOException, InterruptedException {

        String input;
        String[] lines;
        Message msg = new Message();
        System.out.println(inputBuffer.ready());
        while (inputBuffer.ready()) {
            input = inputBuffer.readLine();
            System.out.println(input);
            lines = input.split(MsgParser.SPLIT);
            String key = lines[0];
            switch (key) {
                case MsgParser.UPG:
                    msg.Type = "Handshake";
                    break;
                case MsgParser.SECWSKEY:
                    msg.WebSocketKey = lines[1];
                    break;
                case MsgParser.HOST:
                case MsgParser.USRAGENT:
                case MsgParser.COOKIE:
                case MsgParser.ACC:
                case MsgParser.ACCLANG:
                case MsgParser.ACCENC:
                case MsgParser.DNT:
                case MsgParser.SECWS:
                case MsgParser.ORIGIN:
                case MsgParser.SECWSEXT:
                case MsgParser.CONN:
                case MsgParser.PRAGMA:
                case MsgParser.CC:
                case MsgParser.EMPTY:
                    break;
                default:
                    System.out.println("Unhandled key: " + key);
                    break;
            }
        }
        //for debugging :D
        Thread.sleep(1000);
        return msg;
    }

}
