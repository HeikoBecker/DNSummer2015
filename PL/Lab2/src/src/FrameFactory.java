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
        // TODO: handle fragmentation
        // TODO: handle longer messages where additional payloadlength fields are used
        int headerLength = 2;
        int length = text.length();

        System.out.println(text);

        // Create header
        byte[] result = new byte[headerLength + length];
        result[0] = addFIN(MsgParser.TEXT);
        result[1] = (byte) length;

        // Insert payload
        System.arraycopy(text.getBytes("utf8"), 0, result, headerLength, length);
		return result;
	}
	
	private static byte addFIN (byte OPCode) {
		return (byte) (FIN + OPCode);
	}
}

