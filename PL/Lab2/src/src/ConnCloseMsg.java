import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;


public class ConnCloseMsg extends Message {
	
	public static int NORMAL = 1000;
	public static int GOINGAWAY = 1001;
	public static int PROTERR = 1002;
	public static int UNSUPPDATA = 1003;
	public static int NOSTATUS = 1005;
	public static int ABNCLOSE = 1006;
	public static int INVPAYLOAD = 1007;
	public static int POLICYVIO = 1008;
	public static int MSGTOOBIG = 1009;
	public static int MANEXT = 1010;
	public static int INTERNALERR = 1011;
	public static int TLSHANDSHAKE = 1015;
	
	private int reason;

	/*
	 * Default Constructor, sets close code to 1005 as said in
	 * 7.1.5 page 41
	 */
	public ConnCloseMsg(){
		this.reason = NOSTATUS;
	}

	/*
	 * Constructor for given close code.
	 */
	public ConnCloseMsg(int reason){
		this.reason = reason;
	}

	@Override
	public void execute(BufferedOutputStream bw, Socket clientSocket) throws IOException{
		//TODO: Reply needed?
		//FIN = 1?
		// Reserved Bytes
		// Conn Close OPCODE
		// THEN?
        bw.write(FrameFactory.CloseFrame(this.reason));
        bw.flush();
		clientSocket.shutdownInput();
		clientSocket.shutdownOutput();
		clientSocket.close();
	}
}
