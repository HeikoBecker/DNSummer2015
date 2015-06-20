
public class FrameFactory {

	public static byte[] PongFrame () {
		byte[] result = {0,0,0,0,MsgParser.PONG};
		return result;
	}
	
	public static byte[] CloseFrame() {
		byte[] result = {1,0,0,0,MsgParser.CONNCLOSE};
		return result;
	}

	public static char[] testText() {
//		char[] header = {0x0,0x0,0x0,0x0,MsgParser.TEXT};
//		char[] msg = {'H','e','l','l','o',' ','f','r','o','m',' ','S','e','r','v','e','r'};
//		char[] result = new char[header.length + msg.length];
//		System.arraycopy(header, 0, result, 0, header.length);
//		System.arraycopy(msg, 0, result, header.length, msg.length);
//		return result;
		char[] result = { 0x81,0x05, 'H','e','l','l','o'};// 0x05, 0x48, 0x65, 0x6c, 0x6f};
		return result;
	}
	
}
