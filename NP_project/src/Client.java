import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    final int port=4444;
    final String host="localhost";
    Scanner input;


    Socket socket;
    BufferedReader in;
    PrintWriter out;

    private void createSocket(String server, int port) throws IOException {
        socket=new Socket(server,port);
        in=new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out =new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
        input=new Scanner(System.in);


    }

    private void register() throws IOException {
//        BufferedReader in=new BufferedReader(new InputStreamReader(socket.getInputStream()));
//        PrintWriter out =new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
        out.write("register");
        out.flush();
        //Scanner input=new Scanner(System.in);

        System.out.println("Please enter your Username: ");
        String username = input.nextLine();
        System.out.println("Please Enter your password");
        String password = input.nextLine();
        System.out.println("Please Enter your role\n enter (1) for normal , enter 2 for super");
        int option = input.nextInt();
        String role="";
        if(option==1)role="normal";
        else role="super";
        out.write(username + "," + password+","+role);
        out.flush();
        String response = in.readLine();

    }



    private boolean login() throws IOException{

        //create necessary InputStream/OutputSteam objects
//        BufferedReader in=new BufferedReader(new InputStreamReader(socket.getInputStream()));
//        PrintWriter out =new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

        //Scanner input=new Scanner(System.in);

        while(true) {

            out.write("login");
            out.flush();

            System.out.println("Please enter your Username: ");
            String username = input.nextLine();
            System.out.println("Please Enter your password");
            String password = input.nextLine();
            out.write(username + "," + password);
            out.flush();
            String response = in.readLine();
            if (!response.equals("login success")) {
                System.out.println("No user with this username or password....");
                System.out.println("If you need to try again enter 1 \n if you need to Register enter 2");
                int option = input.nextInt();
                if (option == 2)return false;
            }
            else break;
        }
        return true;
    }

    private void upload() throws IOException {

        System.out.println("Enter file name : ");
        String filename=input.nextLine();
        out.println("upload");
        out.flush();
        out.println(filename);
        out.flush();
        String response=in.readLine();
        if(response.equals("failed")){
            System.out.println("No file with this name .....");
        }
        else {
            System.out.println("success uploading..... ");
        }

    }
    private void download() throws IOException {
        System.out.println("Enter file name : ");
        String filename=input.nextLine();
        out.println("download");
        out.flush();
        out.println(filename);
        out.flush();
        String response=in.readLine();
        if(response.equals("failed")){
            System.out.println("No file with this name .....");
        }
        else {
            System.out.println("success downloading ..... ");
        }
    }
    private void delete() throws IOException {
        System.out.println("Enter file name : ");
        String filename=input.nextLine();
        out.println("delete");
        out.flush();
        out.println(filename);
        out.flush();
        String response=in.readLine();
        if(response.equals("failed")){
            System.out.println("No file with this name .....");
        }
        else {
            System.out.println("success deleting ..... ");
        }
    }
    private void list(){
        out.println("list");
        out.flush();

    }



    public void mainClient() {

        try{
            createSocket(host, port);
            boolean check=login();
            if(!check) register();
            System.out.println("You have 4 services : ");

            while (true) {

                System.out.println("Enter 1 if you need to UPLOAD <filename>");
                System.out.println("Enter 2 if you need to DOWNLOAD <filename>");
                System.out.println("Enter 3 if you need to LIST all files");
                System.out.println("Enter 4 if you need to DELETE <filename>");
                System.out.println("if you need to leave enter anything else");

                String option=input.nextLine();

                if (option.equals("1"))upload();
                else if (option.equals("2"))download();
                else if (option.equals("3"))delete();
                else if (option.equals("4"))list();
                else break;


            }





        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            try{
                if(socket!=null)socket.close();
            }catch (IOException e){
                System.out.println("There is an ERROR "+e.getMessage());
            }
        }
    }
}
