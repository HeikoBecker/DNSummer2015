public class RemoteClient {
    private final String userId;
    private final String userName;
    private final String description;
    private final int hopCount;

    public RemoteClient(String id, String userName, String description, int hopCount) {
        this.userId = id;
        this.userName = userName;
        this.description = description;
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

    public String getDescription() {
        return description;
    }
}
