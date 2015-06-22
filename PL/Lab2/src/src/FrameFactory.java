import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;

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
		byte[] result = { addFIN (MsgParser.TEXT), 0x05, 'H','e','l','l','o'};
		return result;
	}

	public static byte[] TextFrame(String text) throws UnsupportedEncodingException {
        byte opcode = addFIN(MsgParser.TEXT);
        byte length = (byte) text.length();
        String frame = opcode + "" + length;
        frame += text.getBytes("utf8");
        System.out.println(frame);

		byte[] result = frame.getBytes();
        System.out.println(DatatypeConverter.printHexBinary(frame.getBytes("utf8")));
		return result;
	}
	
	private static byte addFIN (byte OPCode) {
		return (byte) (FIN + OPCode);
	}
}

