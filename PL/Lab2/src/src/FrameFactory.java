import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class FrameFactory {

    public static byte FIN = (byte) 0b10000000;

    public static byte[] PongFrame() {
        return new byte[]{addFIN(MsgParser.PONG)};
    }

    public static byte[] CloseFrame(int reason) {
        return new byte[]{addFIN(MsgParser.CONNCLOSE), (byte) (reason << 16), (byte) reason};
    }

    public static byte[] TextFrame(String text) throws UnsupportedEncodingException {
        /*
         * Note: We intentionally do not support text frames that are longer than the value range of int.
         * As the reference implementation does not support dn.messages longer than 384, there is no need for this.
         */

        int headerLength = 2;
        byte[] payLoadData = text.getBytes(StandardCharsets.UTF_8);
        int length = payLoadData.length;
        if (length >= 126) { // Using 126 as payload length, we need 2 additional bytes as length.
            headerLength = 4;
        }
        if (length >= 65536) { // Using 127 as payload length, we need 8 additional bytes as length.
            headerLength = 10;
        }

        // Create header
        byte[] result = new byte[headerLength + length];
        result[0] = addFIN(MsgParser.TEXT);
        result[1] = (byte) length;
        if (length >= 126) {
            result[1] = 126;
            result[2] = (byte) ((length >> 8) & 0x0FF);
            result[3] = (byte) (length & 0x0FF);
        }
        if (length >= 65536) {
            result[1] = 127;
            result[2] = (byte) ((length >> 56) & 0x0FF);
            result[3] = (byte) ((length >> 48) & 0x0FF);
            result[4] = (byte) ((length >> 40) & 0x0FF);
            result[5] = (byte) ((length >> 32) & 0x0FF);
            result[6] = (byte) ((length >> 24) & 0x0FF);
            result[7] = (byte) ((length >> 16) & 0x0FF);
            result[8] = (byte) ((length >> 8) & 0x0FF);
            result[9] = (byte) ((length) & 0x0FF);
        }

        // Insert payload
        System.arraycopy(payLoadData, 0, result, headerLength, length);
        return result;
    }

    private static byte addFIN(byte OPCode) {
        return (byte) (FIN + OPCode);
    }
}

