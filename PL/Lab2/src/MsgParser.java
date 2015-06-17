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
    private static final String GET = "GET / HTTP/1.1";
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
    private InputStreamReader sr;
    private InputStream is;

    public MsgParser(InputStream is) {
        this.is = is;
        this.sr = new InputStreamReader(new DataInputStream(is));
        this.inputBuffer = new BufferedReader(this.sr);
    }

    public Message getHTTPMessage() throws IOException, InterruptedException {
        String input;
        String[] lines;
        Message msg = new Message();
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
                case MsgParser.SECWSEXT:
                    msg.WebSocketExtensions = lines[1];
                case MsgParser.HOST:
                case MsgParser.USRAGENT:
                case MsgParser.COOKIE:
                case MsgParser.ACC:
                case MsgParser.ACCLANG:
                case MsgParser.ACCENC:
                case MsgParser.DNT:
                case MsgParser.SECWS:
                case MsgParser.ORIGIN:
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
        return msg;
    }

    public Message getWebsocketMessage() throws IOException {
        Message msg = new Message();
        msg.Type = "Websocket";

        int count = 0;
        int c;
        int payloadlength = 125;
        int maskingKey = 0;
        int idx = 0;
        int opcode = -1;

        byte[] payload = new byte[payloadlength];

        while ((c = is.read()) != -1 && count <= payloadlength) {
            count++;
            if (count == 1) {
                opcode = c & 0x0F;
                System.out.println(opcode);
            } else if (count == 2) {
                payloadlength = c & 0x7F;
                payload = new byte[payloadlength];
            } else if (count == 11) {
                maskingKey |= c << 25;
            } else if (count == 12) {
                maskingKey |= c << 16;
            } else if (count == 13) {
                maskingKey |= c << 8;
            } else if (count == 14) {
                maskingKey |= c;
            } else {
                payload[payloadlength - idx - 1] = (byte) c;
                idx++;
            }
        }
        switch (opcode) {
            case 1:
                String text = new String(payload, "UTF-8");
                System.out.println("A TEXT FRAME");
                System.out.println(text);
                break;
            default:
                System.out.println("Unknown opcode: " + opcode);
                break;
        }
        System.out.println("READ COMPLETE MESSAGE");
        return null;
    }
}
