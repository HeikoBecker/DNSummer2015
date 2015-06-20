
public class FrameFactory {

	public static byte[] PongFrame () {
		byte[] result = {0,0,0,0,MsgParser.PONG};
		return result;
	}
	
	public static byte[] CloseFrame() {
		byte[] result = {1,0,0,0,MsgParser.CONNCLOSE};
		return result;
	}
	
}
