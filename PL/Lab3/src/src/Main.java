import java.io.IOException;
import java.util.LinkedList;
import java.util.Scanner;

public class Main {
    private static final boolean DEBUG = false;
    private static LinkedList<Thread> openConnections = new LinkedList<>();
    private static LinkedList<ConnectionThread> openConnectionThreads = new LinkedList<>();

    public static void main(String[] args) throws IOException {
        System.out.println("dnChat is getting started!");
        int listenPort = Chat.DEFAULT_PORT;
        if(args.length > 0) {
            listenPort = Integer.parseInt(args[0]);
        }

        WelcomeThread wt = new WelcomeThread(listenPort);
        Thread t = new Thread(wt);
        t.start();
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
        //First stop accepting new connections
        wt.exit();
        t.interrupt();

        for(ConnectionThread ct : openConnectionThreads) {
            try {
                ct.exit();
            } catch (InternalServerException e) {
                e.printStackTrace();
            }
        }

        for(Thread th : openConnections) {
            th.interrupt();
        }

        Chat.getInstance().stopTimer();

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
            ConnectionThread ct = new ConnectionThread(parts[1], connectPort);
            Thread t = new Thread(ct);
            openConnections.add(t);
            openConnectionThreads.add(ct);
            t.start();
        } catch (IOException e) {
            if(DEBUG) {
                e.printStackTrace();
            }
        }
    }
}
