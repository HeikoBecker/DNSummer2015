package dn.threads;

import dn.Client;

import java.io.IOException;
import java.net.Socket;

public class ConnectionThread extends Thread {

    Client connection;
    final Socket clientSocket;

    public ConnectionThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            connection = new Client(clientSocket);
            connection.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
