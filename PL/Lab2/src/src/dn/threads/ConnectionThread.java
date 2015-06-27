package dn.threads;

import dn.Client;

import java.io.IOException;
import java.net.Socket;

/*
 * @class ConnectionThread
 * A Thread to handle communication on a single TCP connection between a client 
 * and a server.
 */
public class ConnectionThread extends Thread {

    Client connection;
    final Socket clientSocket;

    public ConnectionThread(Socket clientSocket) {
    	System.out.println("New connection established");
    	System.out.flush();;
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
        	//create a new client and let it execute
            connection = new Client(clientSocket);
            connection.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
