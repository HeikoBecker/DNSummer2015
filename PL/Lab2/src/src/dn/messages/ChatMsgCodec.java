package dn.messages;

import dn.messages.chat.AcknChatMsg;
import dn.messages.chat.AuthChatMsg;
import dn.messages.chat.InvdMsg;
import dn.messages.chat.SendChatMsg;

/*
 * Static methods to decode Client messages into high level objects
 * and encode high level objects into strings for sending.
 */
public class ChatMsgCodec {
    public static Message decodeClientMessage(String msg) {
        String[] lines = msg.split("\r\n");
        // A message has at least one line.
        if(lines.length == 0) {
            return new InvdMsg();
        }
        String[] header = lines[0].split(" ");
        // A message MUST contain a command and an id.
        if(header.length != 2) {
            return new InvdMsg();
        }

        String command = header[0];
        String id = header[1];

        switch (command) {
            case "AUTH":
                // An AUTH message must have 3 lines.
                if(lines.length != 3) {
                    return new InvdMsg();
                }
                String name = lines[1];
                String password = lines[2];
                return new AuthChatMsg(id, name, password);
            case "SEND":
                // An ACKN message must have 3 lines.
                if(lines.length != 3) {
                    return new InvdMsg();
                }
                String recipient = lines[1];
                String message = lines[2];
                return new SendChatMsg(id, recipient, message);
            case "ACKN":
                // An ACKN message must have 1 line.
                if(lines.length != 1) {
                    return new InvdMsg();
                }
                return new AcknChatMsg(id);
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
