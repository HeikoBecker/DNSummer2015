public class ChatMsgFactory {
    public static Message createClientMessage(String msg) {
        String[] lines = msg.split("\n");
        String[] header = lines[0].split(" ");
        String command = header[0];
        Long id = Long.parseLong(header[1].trim());
        System.out.println(command);
        System.out.println(id);

        switch(command)
        {
            case "AUTH":
                String name = lines[1];
                String password = lines[2];
                return new AuthMsg(id, name, password);
            default:
                System.out.println(command + " is not yet handled by the server.");
                break;
        }

        return null;
    }

    public static String createResponse(String command, Long id, String[] lines) {
        String response = command + " " + id;
        for(int i = 0; i < lines.length; i++) {
            response += "\n" + lines[i];
        }
        return response;
    }
}
