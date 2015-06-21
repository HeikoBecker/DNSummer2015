import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

public class DNConnection {

	private Socket clientSocket;
	PrintWriter pr;
	MsgParser parser;
	private BufferedOutputStream bw;
	private boolean serverShutdown;

	public DNConnection(Socket clientSocket) {
		try {
			this.clientSocket = clientSocket;
			this.pr = new PrintWriter(clientSocket.getOutputStream(), true);
			this.bw = new BufferedOutputStream(clientSocket.getOutputStream());
			System.out.println("[WS] Incoming socket!");
			this.parser = new MsgParser(clientSocket.getInputStream());
			this.serverShutdown = false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void run() {
		try {

			Message handshake = parser.getHTTPMessage();

			// TODO: Maybe make this a factory!
			String base64Token = DNConnection.getSecToken(handshake.WebSocketKey);
			String welcomeMsg = "HTTP/1.1 101 Switching Protocols\n"
					+ "Upgrade: websocket\n" + "Connection: Upgrade\n"
					+ "Sec-WebSocket-Accept: " + base64Token;
			welcomeMsg += "\r\n\r\n";
			System.out.print(welcomeMsg);

			pr.print(welcomeMsg);
			pr.flush();

			System.out.println("[WS] Handshake complete!");
			
			System.out.println("Testing message sending");
//			byte[] testText = FrameFactory.testText();
//			byte[] closeText = FrameFactory.CloseFrame(ConnCloseMsg.NOSTATUS);
//			for (int i = 0; i < testText.length; i++){
//				System.out.println(testText[i]);
//			}
//			bw.write(testText);
//			bw.flush();
//			bw.write(closeText);
//			bw.flush();
			while (!clientSocket.isClosed() || this.serverShutdown) {
				Message message2 = parser.getWebsocketMessage();
				//System.out.println(message2);
				// Let new message execute, resp. send messages on socket
				//message2.execute(pr, clientSocket);
			}
		} catch (IOException e) {
			System.out.println(e);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void tellShutdown() {
		synchronized(this){
			this.serverShutdown = true;
		}
	}
	
    /*
     * TODO: What does this method do?
     */
	private static String getSecToken(String token)
			throws NoSuchAlgorithmException, UnsupportedEncodingException {
		token += "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
		MessageDigest cript = MessageDigest.getInstance("SHA-1");
		cript.reset();
		cript.update(token.getBytes("utf8"));
		return DatatypeConverter.printBase64Binary(cript.digest());
	}

}
