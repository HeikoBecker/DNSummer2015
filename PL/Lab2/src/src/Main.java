import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {
        System.out.println("dnChat is getting started!");
        Thread wt = new Thread(new WelcomeThread());
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
        // TODO: should be cleaner
        wt.interrupt();
        //Close Scanner to avoid resource leak
        sc.close();
    }
}
