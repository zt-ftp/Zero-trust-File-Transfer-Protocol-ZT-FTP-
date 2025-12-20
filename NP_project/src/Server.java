import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Server extends Thread {

    private Socket client;
    PrintWriter write;
    BufferedReader read ;

    public Server(Socket accept) throws IOException {
        client=accept;
        write = new PrintWriter(this.client.getOutputStream());
        read = new BufferedReader(new InputStreamReader(this.client.getInputStream()));
    }



    private void login(String username,String password){



    }
    private void register(String username,String password,String role){


    }

    public void run(){


    }
}
