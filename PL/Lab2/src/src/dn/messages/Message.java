package dn.messages;

import dn.Client;

import java.io.IOException;

public abstract class Message {
    public String id;
    public String Type;

    public Message() { }

    @Override
    public String toString() {
        return Type + " " + id;
    }

    public abstract void execute(Client client) throws IOException;

}
