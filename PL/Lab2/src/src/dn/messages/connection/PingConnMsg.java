package dn.messages.connection;

import dn.Client;
import dn.messages.FrameFactory;
import dn.messages.Message;

import java.io.IOException;


public class PingConnMsg extends Message {

	@Override
	public void execute(Client connection) throws IOException {
		connection.sendFrame(FrameFactory.PongFrame());
	}
}
