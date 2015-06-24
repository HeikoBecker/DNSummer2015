package dn.messages.connection;

import dn.Client;
import dn.messages.Message;

import java.io.IOException;


public class PongConnMsg extends Message {

    @Override
    public void execute(Client client) throws IOException {
        //PAGE 37, RFC 6455
        //PONG frame is unidirectional heartbeat --> no sending
        //on receipt
    }

    //TODO: Method to notify server that client is awake!
}
