import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/*
 * Static methods to decode Client messages into high level objects
 * and encode high level objects into strings for sending.
 */
public class ChatMsgCodec {
    public static Message decodeMessage(boolean isClient, String msg) {
        String[] lines = msg.split("\r\n");
        // A client message has at least one line.
        if (isClient && lines.length == 0) {
            return new InvdMsg();
        }
        String[] header = lines[0].split(" ");
        // A message MUST contain a command and an id.
        if (header.length != 2) {
            return new InvdMsg();
        }

        String command = header[0];
        String id = header[1];

        switch (command) {
            case "AUTH":
                // An AUTH message must have 3 lines. It cannot be sent by a Server, but at this early stage, we don't know if it is a server or not.
                if (lines.length != 3) {
                    return new InvdMsg();
                }
                String name = lines[1];
                String password = lines[2];
                return new AuthChatMsg(id, name, password);
            case "SEND":
                if (!isClient /* TODO: add this "&& lines.length != 4" */) {
                    // TODO: implement SEND by server
                    throw new NotImplementedException();
                }

                // A SEND message must have 3 lines, when sent by a client.
                if (isClient && lines.length != 3) {
                    return new InvdMsg();
                }

                String recipient = lines[1];
                String message = lines[2];
                return new SendChatMsg(id, recipient, message);
            case "ACKN":
                if (!isClient /* TODO: add this "&& lines.length != 3" */) {
                    // TODO: implement ACKN by server
                    throw new NotImplementedException();
                }

                // An ACKN message must have 1 line.
                if (lines.length != 1) {
                    return new InvdMsg();
                }
                return new AcknChatMsg(id);
            case "SRVR":
                if (isClient) {
                    return new InvdMsg();
                }
                return new SrvrChatMsg();
            case "ARRV":
                if (isClient || lines.length != 4) {
                    return new InvdMsg();
                }

                String userName = lines[1];
                String description = lines[2];
                int hopCount = Integer.parseInt(lines[3]);
                return new ArrvChatMsg(id, userName, description, hopCount);
            case "LEFT":
                if (isClient || lines.length != 0) {
                    return new InvdMsg();
                }
                // TODO: implement LEFT by server
                throw new NotImplementedException();
            default:
                return new InvdMsg();
        }
    }

    public static String encodeServerMessage(String command, String id, String[] lines) {
        String response = command + " " + id;
        for (int i = 0; i < lines.length; i++) {
            response += "\r\n" + lines[i];
        }
        return response;
    }
}