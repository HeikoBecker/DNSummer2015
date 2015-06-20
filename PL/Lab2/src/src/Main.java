
public class Main {

    public static void main(String[] args) {
    	//TODO: Make this a parameter? Research in specification
        int port = 4711;
        System.out.println("dnChat is getting started!");
        
        DNConnection connection = new DNConnection(port);
        connection.run();
    }
}
