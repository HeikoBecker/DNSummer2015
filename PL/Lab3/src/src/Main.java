import java.io.IOException;
import java.util.LinkedList;
import java.util.Scanner;

public class Main {
    private static final boolean DEBUG = false;
    private static LinkedList<Thread> openConnections = new LinkedList<>();

    public static void main(String[] args) throws IOException {
        System.out.println("dnChat is getting started!");
        int listenPort = Chat.DEFAULT_PORT;
        if(args.length > 0) {
            listenPort = Integer.parseInt(args[0]);
        }

        Thread wt = new Thread(new WelcomeThread(listenPort));
        wt.setDaemon(true);
        wt.start();
        System.out.println("Type in \"exit\" to stop the server or \"connect <host> [port]\" to connect to another server.");

        Scanner sc = new Scanner(System.in);
        while(sc.hasNextLine()) {
            String line = sc.nextLine();
            if (line.equals("exit")) {
                break;
            }
            if(line.startsWith("connect")) {
                connect(line);
            }
        }
        wt.interrupt();

        // TODO: close all connection threads to other servers; only works in one direction
        // TODO: We must send a left for each! user that we advertised a route 
        for(Thread t : openConnections) {
            t.interrupt();
        }

        // Close Scanner to avoid resource leak
        sc.close();
    }

    private static void connect(String line) {
        String[] parts = line.split(" ");
        if(!(parts.length == 2 || parts.length == 3)) {
            System.out.println("Please enter a command of the form \"connect <host> [port]\".");
            return;
        }

        int connectPort = Chat.DEFAULT_PORT;
        if(parts.length == 3) {
            connectPort = Integer.parseInt(parts[2]);
        }

        try {
            Thread ct = new Thread(new ConnectionThread(parts[1], connectPort));
            ct.setDaemon(true);
            openConnections.add(ct);
            ct.start();
        } catch (IOException e) {
            if(DEBUG) {
                e.printStackTrace();
            }
        }
    }
}
