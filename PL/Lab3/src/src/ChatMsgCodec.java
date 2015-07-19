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
                // An AUTH message must have 3 lines. It cannot be sent by a Server,
                // but at this early stage, we don't know if it is a server or not.
                if (lines.length != 3) {
                    return new InvdMsg();
                } else {
                    String name = lines[1];
                    String password = lines[2];
                    return new LocalAuthChatMsg(id, name, password);
                }
            case "SEND":
                if (isClient) {
                    // A SEND message must have 3 lines, when sent by a client.
                    if (lines.length != 3) {
                        return new InvdMsg();
                    }
                    String recipient = lines[1];
                    String message = lines[2];
                    return new LocalSendChatMsg(id, recipient, message);

                } else {
                    String recipient = lines[1];
                    if (lines.length != 4) {
                        return new InvdMsg();
                    }
                    String senderId = lines[2];
                    String message = lines[3];
                    return new RemoteSendChatMsg(id, recipient, senderId, message);
                }
            case "ACKN":
                if (isClient) {
                    if (lines.length != 1) {
                        return new InvdMsg();
                    }
                    return new LocalAcknChatMsg(id);
                } else {
                    if (lines.length != 3) {
                        return new InvdMsg();
                    }
                    String acknUserId = lines[1];
                    String senderUserId = lines[2];
                    return new RemoteAcknChatMsg(id, acknUserId, senderUserId);
                }
            case "SRVR":
                if (isClient) {
                    return new InvdMsg();
                } else {
                    return new RemoteSrvrChatMsg();
                }
            case "ARRV":
                if (isClient || lines.length != 4) {
                    return new InvdMsg();
                } else {
                    String userName = lines[1];
                    String description = lines[2];
                    int hopCount = Integer.parseInt(lines[3]);
                    //new ARRV spec wants us to return a LEFT for hopCount = 16
                    if (hopCount == 16) {
                        return new RemoteLeftChatMsg(id);
                    }
                    //in the else case, we have a "real" remote ARRV
                    else {
                        return new RemoteArrvChatMsg(id, userName, description, hopCount);
                    }
                }
            case "LEFT":
                if (isClient || lines.length != 1) {
                    return new InvdMsg();
                } else {
                    return new RemoteLeftChatMsg(id);
                }
            default:
                return new InvdMsg();
        }
    }

    public static String encodeServerMessage(String command, String id,
                                             String[] lines) {
        String response = command + " " + id;
        for (int i = 0; i < lines.length; i++) {
            response += "\r\n" + lines[i];
        }
        return response;
    }
}