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
	private static final String SPLIT = ":";
	
	private BufferedReader inputBuffer;
	
	public MsgParser (InputStream is){
		this.inputBuffer = new BufferedReader(new InputStreamReader(new DataInputStream(is)));
	}

	public String getMsg() throws IOException, InterruptedException {
		String input;
		String[] lines;
		while (inputBuffer.ready()){
			input = inputBuffer.readLine();
			lines = input.split(MsgParser.SPLIT);
			switch (lines[0]){
			case MsgParser.HOST :
			case MsgParser.USRAGENT:
			case MsgParser.ACC:
			case MsgParser.ACCLANG:
			case MsgParser.ACCENC:
			case MsgParser.DNT:
			case MsgParser.SECWS:
			case MsgParser.ORIGIN:
			case MsgParser.SECWSEXT:
			case MsgParser.SECWSKEY:
			case MsgParser.CONN:
			case MsgParser.PRAGMA:
			case MsgParser.CC:
			case MsgParser.UPG:
			case MsgParser.EMPTY:
				break;		
			default:
				System.out.println("Unhandled flag " + lines[0]);
				break;
				
			
			}
		}
		//for debugging :D
		//Thread.sleep(1000);
		return "";
	}
	
}
