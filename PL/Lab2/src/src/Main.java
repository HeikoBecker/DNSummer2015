import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;


public class Main {

    public static void main(String[] args) {
    	//TODO: Make this a parameter? Research in specification
        int port = 4711;
        System.out.println("dnChat is getting started!");
        LinkedList<Thread> connections = new LinkedList<Thread>();
        
        try {
			ServerSocket socket = new ServerSocket(port);
			System.out.println("[WS] Socket bound on port " + port + ".");
			while (true) {
				final Socket clientSocket = socket.accept();
				Thread conn = new Thread(){
					
					DNConnection connection;
					
					@Override
					public void run(){
						connection = new DNConnection(clientSocket);
						connection.run();
					}
					
					public void tellShutdown(){
						this.connection.tellShutdown();
					}
				};
				conn.start();
				connections.add(conn);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}
