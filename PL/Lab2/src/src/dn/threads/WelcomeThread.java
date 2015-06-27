package dn.threads;

import java.io.IOException;
import java.net.ServerSocket;

/*
 * @class WelcomeThread
 * Welcomes new arriving connection requests by spawning a new connection 
 * handling thread for each request received on the bound server socket.
 */
public class WelcomeThread implements Runnable {
    private final boolean DEBUG = false;

    @Override
    public void run() {
        int port = 42015; // using the dnChat protocol's default port.

        try {
            //First create the server port
            ServerSocket socket = new ServerSocket(port);
            if (DEBUG) {
                System.out.println("[TCP] Welcome Socket bound on port " + port + ".");
            }

            //Accept all incoming connections on the server socket.
            //socket.accept() blocks hence there is no busy wait
            while (true) {
                ConnectionThread conn = new ConnectionThread(socket.accept());
                conn.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}