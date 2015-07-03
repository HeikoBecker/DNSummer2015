import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {
        System.out.println("dnChat is getting started!");
        int port = 42015;
        if(args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        Thread wt = new Thread(new WelcomeThread(port));
        wt.setDaemon(true);
        wt.start();
        System.out.println("Type in \"exit\" to stop the server.");

        Scanner sc = new Scanner(System.in);
        while(sc.hasNextLine()) {
            String line = sc.nextLine();
            if (line.equals("exit")) {
                break;
            }
        }
        wt.interrupt();
        // Close Scanner to avoid resource leak
        sc.close();
    }
}
