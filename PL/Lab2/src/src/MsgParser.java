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
    public static final byte PING=9;
    public static final byte PONG=10;

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
        byte[] maskingKey = new byte[4];
        int idx = 0;
        int opcode = -1;

        byte[] payload = new byte[payloadlength];
        String line = "";
        while (this.inputBuffer.ready()){
        	line += this.inputBuffer.readLine();
        }
        System.out.println(line);
        while (count-5 <= payloadlength && (c = is.read()) != -1) {
        	System.out.println("Loop");
            count++;
            switch (count){
            case 1:
            	opcode = c & 0x0F;
            	break;
            case 2:
            	System.out.println("Masking "+ c);
            	int tmp = c & 0b10000000;
            	byte mask = (byte) tmp;
            	System.out.println(mask);
            	if (mask == -128)
            		System.out.println("Masking incorporated");
            	else
            		System.out.println("No masking");
            	payloadlength = c & 0b01111111;
            	payload = new byte[payloadlength];
            	System.out.println("Payload " + payloadlength);
            	break;
            case 3:
            	maskingKey[0] = (byte)c;
            	System.out.println("Mask1 read");
            	break;
            case 4:
            	maskingKey[1] = (byte)c;
            	System.out.println("Mask2 read");
            	break;
            case 5:
            	maskingKey[2] = (byte)c;
            	System.out.println("Mask3 read");
            	break;
            case 6:
            	maskingKey[3] = (byte)c;
            	System.out.println("Mask4 read");
            	break;
            default:
            	//TODO: Hier muss das entmaskieren aus dem RFC hin
            	payload[idx] = (byte) ((byte)c ^ maskingKey[idx % 4]);
            	idx++;
            	System.out.println("Payload "+idx+ " read");
            	break;
            }
        }
//            if (count == 2) {
//                payloadlength = c & 0x7F;
//                payload = new byte[payloadlength];
//            } else if (count == 11) {
//                maskingKey |= c << 24;
//            } else if (count == 12) {
//                maskingKey |= c << 16;
//            } else if (count == 13) {
//                maskingKey |= c << 8;
//            } else if (count == 14) {
//                maskingKey |= c;
//            } else {
//                payload[payloadlength - idx - 1] = (byte) c;
//                idx++;
//            }
//        }
        System.out.println(count);
        switch (opcode) {
        	//Continuation Frame according to RFC (opcode is 0, Page 32++)
        	//Cleanly close conn in this case
        	case CONT:
        		System.out.println("TODO: Close connection on Continuation!");
        		return new ConnCloseMsg(1007); //INV PAYLOAD DATA (Sec. 11.7, Page 64)
            case TEXT:
                String text = new String(payload, "UTF-8");
                System.out.println("A TEXT FRAME");
                System.out.println(text);
                break;
            case BIN:
            	System.out.println("TODO: Handle binary frame!");
            	break;
            case CONNCLOSE:
            	System.out.println("A CONNECTION CLOSE");
            	if (opcode == -1)
            		return new ConnCloseMsg();
            	else
            		//TODO: Check for correctness of opcode
            		return new ConnCloseMsg(opcode);
            	//TODO: Finally you must close the connection
            	//TODO: Server must close first
            case PING:
            	System.out.println("TODO: Handle PING frame!");
            	return new PingMsg();
            case PONG:
            	System.out.println("TODO: Handle Pong frame!");
            	return new PongMsg();
            //TODO: Find out what to do for unspecified opcodes
            default:
                System.out.println("Unknown opcode: " + opcode);
                break;
        }
        System.out.println("READ COMPLETE MESSAGE");
        return null;
    }
}
