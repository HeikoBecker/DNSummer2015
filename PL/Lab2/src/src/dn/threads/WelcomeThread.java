package dn.threads;

import java.io.IOException;
import java.net.ServerSocket;

public class WelcomeThread implements Runnable {
    @Override
    public void run() {
        int port = 42015; // using the dnChat protocol's default port.

        try {
            ServerSocket socket = new ServerSocket(port);
            System.out.println("[WS] Welcome Socket bound on port " + port + ".");

            while (true) {
                ConnectionThread conn = new ConnectionThread(socket.accept());
                conn.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}