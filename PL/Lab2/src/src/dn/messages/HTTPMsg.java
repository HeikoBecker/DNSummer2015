package dn.messages;

import dn.Client;

import java.io.IOException;
import java.util.Base64;


public class HTTPMsg extends Message {
    public String WebSocketExtensions;
    private String WebSocketKey;
    public boolean isCorrectProtocol;
	public boolean hostSet;
	public boolean invalid;

	public HTTPMsg(){
		this.invalid = false;
		this.hostSet = false;
		this.isCorrectProtocol = false;		
	}
	
    @Override
    public void execute(Client client) throws IOException {

    }

	public void setWebSocketKey(String wskey) {
		byte[] decoded = Base64.getDecoder().decode(wskey);
		//4.2.1 5.
		if (decoded.length == 16){
			this.WebSocketKey = wskey;
		}else{
			this.invalid = true;
		}
	}

	public String getWebSocketKey() {
		return this.WebSocketKey;
	}
	
	public boolean isInvalid(){
		return invalid || !hostSet || !isCorrectProtocol; 
	}
}
