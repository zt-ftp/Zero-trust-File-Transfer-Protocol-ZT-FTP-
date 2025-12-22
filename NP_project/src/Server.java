import java.io.*;
import java.net.Socket;
import java.util.*;

public class Server extends Thread {

    private Socket client;
    private PrintWriter write;
    private BufferedReader read ;

     private Map<String,User> users;
     private Map<String, List<String>> files;


    public Server(Socket accept) throws IOException {
        client=accept;
        write = new PrintWriter(this.client.getOutputStream());
        read = new BufferedReader(new InputStreamReader(this.client.getInputStream()));
        users=new HashMap<>();
        files=new HashMap<>();
    }


    public void loadUsersFromFile(String pathFile) throws Exception {
        Properties props=new Properties();

        try{

            FileInputStream file=new FileInputStream(pathFile);
            props.load(file);
        }catch (FileNotFoundException e){
            throw new Exception("No file with this path ......");
        }

        for (String username :props.stringPropertyNames() ){
            String value=props.getProperty(username).trim();

            String []userinfo=value.split(",");
            if (userinfo.length!=2)continue;
            users.put(username, new User(username,userinfo[0].trim(),userinfo[1].trim() ));
        }

    }
    public void loadUsersFilesFromFile(String pathFile) throws Exception {
        Properties props=new Properties();

        try{
            FileInputStream file=new FileInputStream(pathFile);
            props.load(file);
        }catch (FileNotFoundException e){
            throw new Exception("No file with this path ......");
        }
        for (String username :props.stringPropertyNames() ){
            String value=props.getProperty(username).trim();

            String []filesList=value.split(",");
            files.put(username,List.of(filesList));
        }

    }
    public void addNewUser(User user){
        users.put(user.getUsername(), new User(user.getUsername(),user.getPassword(),user.getRole()));
    }
    public boolean checkUserExists(String username){
        return users.containsKey(username);
    }
    public boolean checkUserFilesExists(String username){

        return !files.get(username).isEmpty();
    }
    private void login(String username,String password) throws Exception {

        if(!checkUserExists(username))throw new Exception("No user with this username.....");
        if (!users.get(username).getPassword().equals(password))throw new Exception("Wrong password.....");
        //you should send a sesstion key to the client here(read the docs)
        System.out.println("The user "+username+" login in the system ....");


    }
    private void register(String username,String password,String role){
        users.put(username,new User(username,password,role));
        //you should send a sesstion key to the client here(read the docs)
        //also you should save the new user in the users file

    }

    public void authenticate()throws IOException  {
        //            while (true) {
        String command = read.readLine();
        if (command.equals("login")) {
            String credentials = read.readLine();
            String[] parts = credentials.split(",");
            try {
                login(parts[0], parts[1]);
                write.write("login success\n");
                write.flush();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                write.write(e.getMessage() + "\n");
                write.flush();
            }
        } else if (command.equals("register")) {
            String credentials = read.readLine();
            String[] parts = credentials.split(",");
            register(parts[0], parts[1], parts[2]);
            write.write("register success\n");
            write.flush();
        }
//            }
    }

    public void run(){
        try {
            loadUsersFromFile("users.properties");
            loadUsersFilesFromFile("filesForUsers.properties");
            authenticate();



        } catch (Exception e) {
            System.out.println(e.getMessage());
        }



    }
}
