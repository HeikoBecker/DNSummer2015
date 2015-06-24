package dn.messages;

import dn.messages.connection.CloseConnMsg;
import dn.messages.connection.PingConnMsg;
import dn.messages.connection.PongConnMsg;

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

    public static final byte CONT = 0;
    public static final byte TEXT = 1;
    public static final byte BIN = 2;
    public static final byte CONNCLOSE = 8;
    public static final byte PING = 9;
    public static final byte PONG = 10;

    private BufferedReader inputBuffer;
    private InputStream is;

    public MsgParser(InputStream is) {
        this.is = is;
        this.inputBuffer = new BufferedReader(new InputStreamReader(new DataInputStream(is)));
    }

    public HTTPMsg getHTTPMessage() throws IOException, InterruptedException {
        String input;
        String[] lines;
        HTTPMsg msg = new HTTPMsg();
        while (inputBuffer.ready()) {
            input = inputBuffer.readLine();
            if (input.equals(MsgParser.GET)) {
                msg.isCorrectProtocol = true;
                continue;
            }
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
        return msg;
    }

    public Message getWebsocketMessage() throws IOException {
        Message msg = new UnknownMsg();
        msg.Type = "Websocket";

        int count = 0;
        int c;
        Integer payloadlength = 125;
        byte[] maskingKey = new byte[4];
        int idx = 0;
        int opcode = -1;

        // TODO: check if fin bit is not set. Then the client tries to fragment. In this case we should cleanly close the connection.
        byte[] payload = new byte[payloadlength];
        while (count - 5 <= payloadlength && (c = is.read()) != -1) {
            count++;
            switch (count) {
                case 1:
                    opcode = c & 0x0F;
                    break;
                case 2:
                    payloadlength = c & 0b01111111;
                    // TODO: 126 --> 7+16 bits (as unsigned integer)
                    //Unsigned done with:https://stackoverflow.com/questions/9854166/declaring-an-unsigned-int-in-java
                    if (payloadlength == 126) {
                        c = is.read();
                        payloadlength = c << 8;
                        c = is.read();
                        payloadlength = payloadlength | c;
                    } else if (payloadlength == 127) {
                        // TODO: 127 --> 7+64 bits (as unsigned int)
                        c = is.read();
                        payloadlength = c << 32;
                        c = is.read();
                        payloadlength = payloadlength | (c << 16);
                        c = is.read();
                        payloadlength = payloadlength | (c << 8);
                        c = is.read();
                        payloadlength = payloadlength | c;
                    }
                    payload = new byte[payloadlength]; //TODO: Does this need to be a long for unsigned?
                    break;
                case 3:
                    maskingKey[0] = (byte) c;
                    break;
                case 4:
                    maskingKey[1] = (byte) c;
                    break;
                case 5:
                    maskingKey[2] = (byte) c;
                    break;
                case 6:
                    maskingKey[3] = (byte) c;
                    break;
                default:
                    // TODO: Hier muss das entmaskieren aus dem RFC hin
                    payload[idx] = (byte) ((byte) c ^ maskingKey[idx % 4]);
                    idx++;
                    break;
            }
        }
        switch (opcode) {
            // Continuation Frame according to RFC (opcode is 0, Page 32++)
            // Cleanly close conn in this case
            case CONT:
                return new CloseConnMsg(); // INV PAYLOAD DATA (Sec. 11.7, Page 64)
            case TEXT:
                return ChatMsgCodec.decodeClientMessage(new String(payload, "UTF-8"));
            case CONNCLOSE:
                return new CloseConnMsg();
                // TODO: Finally you must close the connection
                // TODO: Server must close first
            case PING:
                return new PingConnMsg();
            case PONG:
                return new PongConnMsg();
            case BIN:
            default:
                // In this case the parser will return null and this causes the thread to close the socket.
                System.out.println("Unknown opcode: " + opcode);
                break;
        }
        return null;
    }
}