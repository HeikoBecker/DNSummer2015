
public class FrameFactory {

	public static byte FIN = (byte) 0b10000000;
	
	public static byte[] PongFrame () {
		byte[] result = {addFIN(MsgParser.PONG)};
		return result;
	}
	
	public static byte[] CloseFrame(int reason) {
		byte[] result = {addFIN(MsgParser.CONNCLOSE), (byte) (reason << 16), (byte) reason};
		return result;
	}

	public static byte[] testText() {
		byte[] result = { addFIN (MsgParser.TEXT), 0x05, 'H','e','l','l','o'};// 0x05, 0x48, 0x65, 0x6c, 0x6f};
		return result;
	}
	
	private static byte addFIN (byte OPCode) {
		return (byte) (FIN + OPCode);
	}
}
