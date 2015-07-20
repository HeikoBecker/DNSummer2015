import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MsgParser {
    private static final String HOST = "Host";
    private static final String USRAGENT = "User-Agent";
    private static final String ACC = "Accept";
    private static final String ACCLANG = "Accept-Language";
    private static final String ACCENC = "Accept-Encoding";
    private static final String COOKIE = "Cookie";
    private static final String DNT = "DNT";
    //Static String for Get matching. As stated in forum, matching on "/" suffices
    private static final String GET = "GET / HTTP/1.1";
    private static final String SECWS = "Sec-WebSocket-Version";
    private static final String ORIGIN = "Origin";
    private static final String SECWSEXT = "Sec-WebSocket-Extensions";
    private static final String SECWSKEY = "Sec-WebSocket-Key";
    private static final String SECWSACCEPT = "Sec-WebSocket-Accept";
    private static final String CONN = "Connection";
    private static final String PRAGMA = "Pragma";
    private static final String CC = "Cache-Control";
    private static final String UPG = "Upgrade";
    private static final String EMPTY = "";
    private static final String SPLIT = ": ";
    private static final String SWITCHINGPROTO = "HTTP/1.1 101 Switching Protocols";

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
        this.inputBuffer = new BufferedReader(new InputStreamReader(
                new DataInputStream(is)));
    }

    public HTTPMsg getHTTPMessage(boolean isResponse) throws IOException {
        String input;
        String[] lines;
        HTTPMsg msg = new HTTPMsg();

        //first match a GET
        input = inputBuffer.readLine();
        // Check the first line to be equal to "GET / HTTP/1.1" otherwise
        // this cannot be a valid request
        if (!input.equals(MsgParser.GET) && !isResponse) {
            msg.setInvalid();
            return msg;
        }
        //based on RFC. First line MUST be a switching protocol
        if (!input.equals(MsgParser.SWITCHINGPROTO) && isResponse) {
            msg.setInvalid();
            return msg;
        }
        msg.setCorrectProtocol();

        while (inputBuffer.ready()) {
            //Finish loop earlier if we already had a failure
            if (msg.isInvalid())
                break;
            input = inputBuffer.readLine();
            lines = input.split(MsgParser.SPLIT);
            String key = lines[0];
            switch (key) {
                //Duplicate GET --> Failure, may come from browser accessing
                case MsgParser.GET:
                    msg.setInvalid();
                    break;
                // Upgrade Key check
                case MsgParser.UPG:
                    // must be exactly a upgrade to a websocket (RFC 6455 see 4.2.1
                    // 3.)
                    if (lines[1].equals("websocket")) {
                        msg.type = "Handshake";
                    } else {
                        log("Invalid Upgrade");
                        msg.setInvalid();
                    }
                    break;
                //Sec-WebSocket-Key field, must be 16 bytes in length when base64-decoded (4.2.1 5.)
                case MsgParser.SECWSKEY:
                    msg.setWebSocketKey(lines[1]);
                    break;
                case MsgParser.SECWSEXT:
                    msg.WebSocketExtensions = lines[1];
                    break;
                // Check that the host field is set, but ignore it as stated in the
                // forum
                case MsgParser.HOST:
                    msg.hostSet = true;
                    break;
                // Connection Header Field Required as by 4.2.1 4.
                case MsgParser.CONN:
                    boolean ok = false;
                    String[] values = lines[1].split(",");

                    for (String value : values) {
                        value = value.replace(" ", "");
                        if (ok)
                            break;
                        if (value.equals("Upgrade"))
                            ok = true;
                    }
                    if (!ok) {
                        msg.setInvalid();
                        log("Invalid Connection Request: " + lines[1]);
                    }
                    break;
                //Sec-WebSocket-Version - required field, must be 13 (4.2.1 6.)
                case MsgParser.SECWS:
                    if (lines[1].equals("13")) {
                        break;
                    } else {
                        msg.setInvalid();
                        log("Invalid SecWSVersion");
                    }
                    break;
                case SECWSACCEPT:
                    //TODO: See issue #34, shouldn't this be checked for presence?
                    break;
                //Optional, unchecked fields, ignored as stated in forum
                case MsgParser.USRAGENT:
                case MsgParser.COOKIE:
                case MsgParser.ACC:
                case MsgParser.ACCLANG:
                case MsgParser.ACCENC:
                case MsgParser.DNT:
                case MsgParser.ORIGIN:
                case MsgParser.PRAGMA:
                case MsgParser.CC:
                case MsgParser.EMPTY:
                    break;
                //Dead Code, Only for debugging
                default:
                    log("Unhandled key: " + key);
                    msg.setInvalid();
                    break;
            }
        }
        return msg;
    }

    public Message getWebsocketMessage(boolean isDnClient, boolean isWsClient) throws IOException {
        int c;
        int opcode = -1;

        boolean isMasking = false;
        byte[] maskingKey = new byte[4];

        Integer payloadLength = 0;
        byte[] payload = new byte[0];

        // Read Byte 0
        if((c = is.read()) != -1) {
            opcode = c & 0x0F;
            //Check wether FIN bit is set.
            //If not, close connection as we do not support fragmentation
            boolean fin = (c & 0b10000000) == 0b10000000;
            if (!fin)
                return new CloseConnMsg();
        } else {
            log("There was nothing more to read.s");
            return new CloseConnMsg();
        }

        // Read Byte 1
        if((c = is.read()) != -1) {
            // check if mask bit is not set. Then the client tries to send unmasked data.
            // This is forbidden according to the RFC
            // In this case we should cleanly close the connection.
            isMasking = (c & 0b10000000) == 0b10000000;

            if (isWsClient && !isMasking) {
                log("Client not setting mask bit.");
                return new CloseConnMsg();
            }

            //otherwise continue
            payloadLength = c & 0b01111111;
            // T126 --> 7+16 bits (as unsigned integer?)
            // Unsigned done
            // with:https://stackoverflow.com/questions/9854166/declaring-an-unsigned-int-in-java
            if (payloadLength == 126) {
                c = is.read();
                payloadLength = c << 8;
                c = is.read();
                payloadLength = payloadLength | c;
            } else if (payloadLength == 127) {
                // 127 --> 7+64 bits (as unsigned int)
                c = is.read();
                payloadLength = c << 32;
                c = is.read();
                payloadLength = payloadLength | (c << 16);
                c = is.read();
                payloadLength = payloadLength | (c << 8);
                c = is.read();
                payloadLength = payloadLength | c;

                // We deviate from the RFC as the application will only support message up to a length of 300,
                // so there is no need to parse larger messsages.
                // See https://dcms.cs.uni-saarland.de/dn/forum/viewtopic.php?f=3&t=124
                if (payloadLength < 0) {
                    payloadLength = Integer.MAX_VALUE;
                }
            }
            payload = new byte[payloadLength];
        } else {
            log("There was nothing more to read.s");
            return new CloseConnMsg();
        }

        // Read (optional) 4 masking Bytes
        if(isMasking) {
            for(int i = 0; i < 4; i++) {
                if((c = is.read()) != -1) {
                    maskingKey[i] = (byte) c;
                } else {
                    log("There was nothing more to read.s");
                    return new CloseConnMsg();
                }
            }
        }

        // Read Payload Bytes
        for (int i = 0; i < payloadLength; i++) {
            if((c = is.read()) != -1) {
                if(isMasking) {
                    payload[i] = (byte) ((byte) c ^ maskingKey[i % 4]);
                } else {
                    payload[i] = (byte) c;
                }
            } else {
                log("There was nothing more to read.s");
                return new CloseConnMsg();
            }
        }

        switch (opcode) {
            // Continuation Frame according to RFC (opcode is 0, Page 32++)
            // Cleanly close conn in this case
            case CONT:
                return new CloseConnMsg(); // INV PAYLOAD DATA (Sec. 11.7, Page 64)
            case TEXT:
                return ChatMsgCodec
                        .decodeMessage(isDnClient, new String(payload, StandardCharsets.UTF_8));
            case CONNCLOSE:
                return new CloseConnMsg();
            case PING:
                return new PingConnMsg();
            case PONG:
                return new PongConnMsg();
            case BIN:
            default:
                // In this case the parser will close the connection as this part is not specified in the RFC
                log("Unknown opcode: " + opcode);
                return new CloseConnMsg();
        }
    }

    // ----------------- DEBUGGING -----------------
    private final boolean DEBUG = false;

    private void log(String msg) {
        if (DEBUG) {
            System.out.println("[MsgParser] " + msg);
        }
    }

}