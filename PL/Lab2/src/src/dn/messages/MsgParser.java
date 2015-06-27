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
	//Static String for Get matching. As stated in forum, matching on "/" suffices
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
		this.inputBuffer = new BufferedReader(new InputStreamReader(
				new DataInputStream(is)));
	}

	public HTTPMsg getHTTPMessage() throws IOException, InterruptedException {
		String input;
		String[] lines;
		HTTPMsg msg = new HTTPMsg();
		
		//first match a GET
		input = inputBuffer.readLine();
		// Check the first line to be equal to "GET / HTTP/1.1" otherwise
		// this cannot be a valid request
		if (!input.equals(MsgParser.GET)) {
			msg.invalid = true;
			return msg;
		}
		msg.isCorrectProtocol = true;
		
		while (inputBuffer.ready()) {
			//Finish loop earlier if we already had a failure
			if(msg.invalid)
				break;
			input = inputBuffer.readLine();
			lines = input.split(MsgParser.SPLIT);
			String key = lines[0];
			switch (key) {
			//Duplicate GET --> Failure, may come from browser accessing
			case MsgParser.GET:
				msg.invalid = true;
				break;
			// Upgrade Key check
			case MsgParser.UPG:
				// must be exactly a upgrade to a websocket (RFC 6455 see 4.2.1
				// 3.)
				if (lines[1].equals("websocket")) {
					msg.Type = "Handshake";
				} else {
					System.out.println("Invalid Upgrade");					
					msg.invalid = true;
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
				if (lines[1].equals("Upgrade")) {
					break;
				} else {
					msg.invalid = true;
					System.out.println("Invalid Connection Request: "+lines[1]);
				}
				break;
			//Sec-WebSocket-Version - required field, must be 13 (4.2.1 6.)
			case MsgParser.SECWS:
				if(lines[1].equals("13")){
					break;
				}else{
					msg.invalid = true;
					System.out.println("Invalid SecWSVersion");
				}
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
				System.out.println("Unhandled key: " + key);
				msg.invalid = true;
				break;
			}
		}
		return msg;
	}

	public Message getWebsocketMessage() throws IOException {
		int count = 0;
		int c;
		Integer payloadlength = 125;
		byte[] maskingKey = new byte[4];
		int idx = 0;
		int opcode = -1;

		byte[] payload = new byte[payloadlength];
		while (count - 5 <= payloadlength && (c = is.read()) != -1) {
			count++;
			switch (count) {
			case 1:
				opcode = c & 0x0F;
				//Check wether FIN bit is set.
				//If not, close connection as we do not support fragmentation
				boolean fin = (c & 0b10000000) == 0b10000000;
				if (!fin)
					return new CloseConnMsg();
				break;
			case 2:
				// check if mask bit is not set. Then the client tries to send unmasked data.
				//This is forbidden according to the RFC
				// In this case we should cleanly close the connection.
				boolean mask = (c & 0b10000000) == 0b10000000;
				if (!mask)
					return new CloseConnMsg();
				//otherwise continue
				payloadlength = c & 0b01111111;
				// TODO: 126 --> 7+16 bits (as unsigned integer?)
				// Unsigned done
				// with:https://stackoverflow.com/questions/9854166/declaring-an-unsigned-int-in-java
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
				payload = new byte[payloadlength]; // TODO: Does this need to be
													// a long for unsigned?
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
				// Demask the payload as explained in the RFC
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
			return ChatMsgCodec
					.decodeClientMessage(new String(payload, "UTF-8"));
		case CONNCLOSE:
			return new CloseConnMsg();
		case PING:
			return new PingConnMsg();
		case PONG:
			return new PongConnMsg();
		case BIN:
		default:
			// In this case the parser will close the connection as this part is not specified in the RFC
			System.out.println("Unknown opcode: " + opcode);
			return new CloseConnMsg();
		}
	}
}