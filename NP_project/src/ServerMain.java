import java.io.IOException;
import java.net.ServerSocket;

public class ServerMain {
    final static int port=4444;
    public static void main(String[] args) {

        ServerSocket server=null;

        try {
            server=new ServerSocket(port);
            System.out.println("Waiting for client connection ....");
            while (true) {
                Server thread = new Server(server.accept());
                thread.start();

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
