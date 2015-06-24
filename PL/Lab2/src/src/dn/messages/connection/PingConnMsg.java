package dn.messages.connection;

import dn.Client;
import dn.messages.FrameFactory;
import dn.messages.Message;

import java.io.IOException;


public class PingConnMsg extends Message {

	@Override
	public void execute(Client client) throws IOException {
		client.emitFrame(FrameFactory.PongFrame());
	}
}
