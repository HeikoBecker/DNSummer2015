import java.io.UnsupportedEncodingException;

public class FrameFactory {

    public static byte FIN = (byte) 0b10000000;

    public static byte[] PongFrame() {
        byte[] result = {addFIN(MsgParser.PONG)};
        return result;
    }

    public static byte[] CloseFrame(int reason) {
        byte[] result = {addFIN(MsgParser.CONNCLOSE), (byte) (reason << 16), (byte) reason};
        return result;
    }

    public static byte[] TextFrame(String text) throws UnsupportedEncodingException {
        /*
         * Note: We intentionally do not support text frames that are longer than the value range of int.
         * As the reference implementation does not support messages longer than 384, there is no need for this.
         */

        int headerLength = 2;
        int length = text.length();
        if (length > 126) { // Using 126 as payload length, we need 2 additional bytes as length.
            headerLength = 4;
        }
        if (length > 65536) { // Using 127 as payload length, we need 8 additional bytes as length.
            headerLength = 10;
        }

        // Create header
        byte[] result = new byte[headerLength + length];
        result[0] = addFIN(MsgParser.TEXT);
        result[1] = (byte) length;
        if (length > 126) {
            result[1] = 126;
            result[2] = (byte) ((length >> 8) & 0x0F);
            result[3] = (byte) (length & 0x0F);
        }
        if (length > 65536) {
            result[1] = 127;
            result[2] = (byte) ((length >> 56) & 0x0F);
            result[3] = (byte) ((length >> 48) & 0x0F);
            result[4] = (byte) ((length >> 40) & 0x0F);
            result[5] = (byte) ((length >> 32) & 0x0F);
            result[6] = (byte) ((length >> 24) & 0x0F);
            result[7] = (byte) ((length >> 16) & 0x0F);
            result[8] = (byte) ((length >> 8) & 0x0F);
            result[9] = (byte) ((length) & 0x0F);
        }

        // Insert payload
        System.arraycopy(text.getBytes("utf8"), 0, result, headerLength, length);
        return result;
    }

    private static byte addFIN(byte OPCode) {
        return (byte) (FIN + OPCode);
    }
}

