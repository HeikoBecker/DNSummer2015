import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class FrameFactory {
    private static Random random = new Random();

    public static byte FIN = (byte) 0b10000000;

    public static byte[] PongFrame() {
        return new byte[]{addFIN(MsgParser.PONG)};
    }

    public static byte[] CloseFrame(boolean asClient, int reason) {
        byte[] message = {addFIN(MsgParser.CONNCLOSE), (byte) (reason << 16), (byte) reason};
        if(asClient) { message = mask(message); }
        return message;
    }

    public static byte[] TextFrame(boolean asClient, String text) throws UnsupportedEncodingException {
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
        if(asClient) {
            result = mask(result);
        }
        return result;
    }

    private static byte addFIN(byte OPCode) {
        return (byte) (FIN + OPCode);
    }

    private static byte[] mask(byte[] originalMessage) {
        byte[] transformedMessage = new byte[originalMessage.length+4]; // The 4 additional bytes are used to store the masking key.
        int payloadField = originalMessage[1] & 0b01111111; // The payload value in the second byte.

        int headerLength = 2;
        if(payloadField == 126) {
            headerLength = 4;
        }
        else if(payloadField == 127) {
            headerLength = 10;
        }
        originalMessage[1] |= 0b10000000; // Set Masking Bit
        System.arraycopy(originalMessage, 0, transformedMessage, 0, headerLength); // Copy te header.

        // Chose and set masking key.
        byte[] maskingKey = new byte[4];
        random.nextBytes(maskingKey);
        System.arraycopy(maskingKey, 0, transformedMessage, headerLength, 4);

        // Apply masking key to payload and store result in transformed frame.
        for (int i = 0; i < originalMessage.length - headerLength; i++) {
            transformedMessage[headerLength + 4 + i] = (byte) (originalMessage[headerLength + i] ^ maskingKey[i % 4]);
        }
        return transformedMessage;
    }
}

