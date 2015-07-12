public class RemoteClient {
    private final String userId;
    private final String userName;
    private final int hopCount;

    /*
     * TODO: This constructor is used in the Chat Instance to register a "server" client which is announced by an arrv.
     * We should rethink our logic for this.
     * Maybe the server should "aggregate" the clients and each client must allow to "compare" a new hopCount against its own.
     * Another alternative is to add a dummy client that prevents being "run"
     */
    public RemoteClient(String id, String userName, int hopCount) {
        this.userId = id;
        this.userName = userName;
        this.hopCount = hopCount;

    }


    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public int getHopCount() {
        return hopCount;
    }
}
