public class DNChatMsgCodec {
    public static Message decodeClientMessage(String msg) {
        String[] lines = msg.split("\r\n");
        String[] header = lines[0].split(" ");
        String command = header[0];
        String id = header[1];

        switch (command) {
            case "AUTH":
                String name = lines[1];
                String password = lines[2];
                return new AuthMsg(id, name, password);
            case "SEND":
                String recipient = lines[1];
                String message = lines[2];
                return new SendMsg(id, recipient, message);
            case "ACKN":
                return new AcknMsg(id);
            default:
                System.out.println(command + " is not yet handled by the server.");
                break;
        }
        return null;
    }

    public static String encodeServerMessage(String command, String id, String[] lines) {
        String response = command + " " + id;
        for (int i = 0; i < lines.length; i++) {
            response += "\r\n" + lines[i];
        }
        return response;
    }
}
