import java.io.IOException;
import java.util.Base64;


public class HTTPMsg extends Message {
    public String WebSocketExtensions;
    private String WebSocketKey;
    private boolean isCorrectProtocol;
	public boolean hostSet;
	private boolean invalid;
	private boolean UpgradeField;
	private boolean ConnectionField;
	private String encodedNonce;

	public HTTPMsg(){
		this.type = "http-Handshake";
		this.invalid = false;
		this.hostSet = false;
		this.isCorrectProtocol = false;
	}
	
    @Override
    public void execute(Peer peer) throws IOException {

    }

	public void setWebSocketKey(String wskey) {
		byte[] decoded = Base64.getDecoder().decode(wskey);
		//4.2.1 5.
		if (decoded.length == 16){
			this.WebSocketKey = wskey;
		} else {
			this.invalid = true;
		}
	}

	public String getWebSocketKey() {
		return this.WebSocketKey;
	}
	
	public boolean hasValidFields(){
		return UpgradeField && ConnectionField;
	}
	
	public boolean isInvalid(){
		return invalid || !isCorrectProtocol;
	}

	public void setInvalid() {
		this.invalid = true;
	}

	public boolean isHostSet() {
		return hostSet;
	}

    public void setCorrectProtocol() {
        isCorrectProtocol = true;
    }

	public void setUpgrade(boolean b) {
		this.UpgradeField = b;
	}

	public void setConnection(boolean b) {
		this.ConnectionField = b;
	}

	public void setEncodedNonce(String encNonce) {
		this.encodedNonce = encNonce;
	}

	public String getEncodedNonce() {
		return this.encodedNonce;
	}
}
