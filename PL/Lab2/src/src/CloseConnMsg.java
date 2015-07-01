import java.io.IOException;


public class CloseConnMsg extends Message {

    public static int NORMAL = 1000;
    public static int GOINGAWAY = 1001;
    public static int PROTERR = 1002;
    public static int UNSUPPDATA = 1003;
    public static int NOSTATUS = 1005;
    public static int ABNCLOSE = 1006;
    public static int INVPAYLOAD = 1007;
    public static int POLICYVIO = 1008;
    public static int MSGTOOBIG = 1009;
    public static int MANEXT = 1010;
    public static int INTERNALERR = 1011;
    public static int TLSHANDSHAKE = 1015;

    private int reason;

    /*
     * Default Constructor, sets close code to 1005 as said in
     * 7.1.5 page 41
     */
    public CloseConnMsg() {
        this.reason = NOSTATUS;
    }

    /*
     * Constructor for given close code.
     */
    public CloseConnMsg(int reason) {
        this.reason = reason;
    }

    @Override
    public void execute(Client client) throws IOException {
        client.exit();
    }
}
