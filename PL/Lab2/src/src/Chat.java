public class Chat {
    private static Chat instance = null;

    private Chat() {
    }

    public static synchronized Chat getInstance() {
        if (instance == null) {
            instance = new Chat();
        }

        return instance;
    }

    public void authenticateClient() {

    }
}