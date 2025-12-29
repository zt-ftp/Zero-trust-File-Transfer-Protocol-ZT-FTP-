//import java.awt.event.ActionListener;
//import java.io.*;
//import java.net.Socket;
//import java.util.Scanner;
//
//public class Client {
//    final int port=4444;
//    final String host="localhost";
//    Scanner input;
//
//
//    Socket socket;
//    BufferedReader in;
//    PrintWriter out;
//    private String role;
//
//
//    private void createSocket(String server, int port) throws IOException {
//        socket=new Socket(server,port);
//        in=new BufferedReader(new InputStreamReader(socket.getInputStream()));
//        out =new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
//        input=new Scanner(System.in);
//
//
//    }
//
//    private void register() throws IOException {
//        out.write("register");
//        out.flush();
//
//        System.out.println("Please enter your Username: ");
//        String username = input.nextLine();
//        System.out.println("Please Enter your password");
//        String password = input.nextLine();
//        System.out.println("Please Enter your role\n enter (1) for normal , enter 2 for super");
//        int option = input.nextInt();
//        String role="";
//        if(option==1)role="normal";
//        else role="super";
//        out.write(username + "," + password+","+role);
//        out.flush();
//        String response = in.readLine();
//
//    }
//
//
//
//    private boolean login() throws IOException{
//
//
//
//        while(true) {
//
//            out.write("login");
//            out.flush();
//
//            System.out.println("Please enter your Username: ");
//            String username = input.nextLine();
//            System.out.println("Please Enter your password");
//            String password = input.nextLine();
//            out.write(username + "," + password);
//            out.flush();
//            String response = in.readLine();
//            if (!response.equals("login success")) {
//                System.out.println("No user with this username or password....");
//                System.out.println("If you need to try again enter 1 \n if you need to Register enter 2");
//                int option = input.nextInt();
//                if (option == 2)return false;
//            }
//            else break;
//        }
//        return true;
//    }
//
//    private void upload() throws IOException {
//
//        System.out.println("Enter file name : ");
//        String filename=input.nextLine();
////        out.println("upload");
////        out.flush();
//        out.println("upload"+":"+filename);
//        out.flush();
//        String response=in.readLine();
//        if(response.equals("failed")){
//            System.out.println("No file with this name .....");
//        }
//        else {
//            System.out.println("success uploading..... ");
//        }
//
//    }
//    private void download() throws IOException {
//        System.out.println("Enter file name : ");
//        String filename=input.nextLine();
////        out.println("download");
////        out.flush();
//        out.println("download"+":"+filename);
//        out.flush();
//        String response=in.readLine();
//        if(response.equals("failed")){
//            System.out.println("No file with this name .....");
//        }
//        else {
//            System.out.println("success downloading ..... ");
//        }
//    }
//    private void delete() throws IOException {
//        System.out.println("Enter file name : ");
//        String filename=input.nextLine();
////        out.println("delete");
////        out.flush();
//        out.println("delete"+":"+filename);
//        out.flush();
//        String response=in.readLine();
//        if(response.equals("failed")){
//            System.out.println("No file with this name .....");
//        }
//        else {
//            System.out.println("success deleting ..... ");
//        }
//    }
//    private void list(){
//        out.println("list:");
//        out.flush();
//        try {
//            String response = in.readLine();
//            System.out.println("Your files are : ");
//            System.out.println(response);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//
//
//    }
//
//
//
//    public void mainClient() {
//
//        try{
//            createSocket(host, port);
//            boolean check=login();
//            if(!check) register();
//            System.out.println("You have 4 services : ");
//
//            while (true) {
//
//                System.out.println("Enter 1 if you need to UPLOAD <filename>");
//                System.out.println("Enter 2 if you need to DOWNLOAD <filename>");
//                System.out.println("Enter 3 if you need to LIST all files");
//                System.out.println("Enter 4 if you need to DELETE <filename>");
//                System.out.println("if you need to leave enter anything else");
//
//                String option=input.nextLine();
//
//                if (option.equals("1"))upload();
//                else if (option.equals("2"))download();
//                else if (option.equals("3"))delete();
//                else if (option.equals("4"))list();
//                else break;
//
//
//            }
//
//
//
//
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }finally{
//            try{
//                if(socket!=null)socket.close();
//            }catch (IOException e){
//                System.out.println("There is an ERROR "+e.getMessage());
//            }
//        }
//    }
//}


import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.util.Scanner;

public class Client {
    final int port = 4444;
    final String host = "localhost";

    private Scanner input;
    private Socket socket;

    private DataInputStream in;
    private DataOutputStream out;

    private String role;
    private String username;
    private String sessionKey;

    private void createSocket(String server, int port) throws IOException {
        socket = new Socket(server, port);
        in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        input = new Scanner(System.in);
    }

    private void readSessionAndRole() throws IOException {
        String sessionLine = in.readUTF(); // "SESSION <key>"
        if (!sessionLine.startsWith("SESSION ")) throw new IOException("Missing SESSION from server");
        sessionKey = sessionLine.substring("SESSION ".length()).trim();

        String roleLine = in.readUTF(); // "ROLE <role>"
        if (!roleLine.startsWith("ROLE ")) throw new IOException("Missing ROLE from server");
        role = roleLine.substring("ROLE ".length()).trim();
    }

    private void register() throws IOException {
        out.writeUTF("REGISTER");
        out.flush();

        System.out.println("Please enter your Username: ");
        String u = input.nextLine().trim();

        System.out.println("Please Enter your password");
        String password = input.nextLine();

        System.out.println("Please Enter your role\n enter (1) for normal , enter (2) for super");
        int option = Integer.parseInt(input.nextLine());
        String r = (option == 1) ? "normal" : "super";

        out.writeUTF(u + "," + password + "," + r);
        out.flush();

        String response = in.readUTF();
        System.out.println("Server: " + response);
        if (!response.startsWith("OK")) return;

        readSessionAndRole();
        username = u;
    }

    private boolean login() throws IOException {
        while (true) {
            out.writeUTF("LOGIN");
            out.flush();

            System.out.println("Please enter your Username: ");
            String u = input.nextLine().trim();

            System.out.println("Please Enter your password");
            String password = input.nextLine();

            out.writeUTF(u + "," + password);
            out.flush();

            String response = in.readUTF();
            System.out.println("Server: " + response);

            if (!response.startsWith("OK")) {
                System.out.println("If you need to try again enter 1 \n if you need to Register enter 2");
                String option = input.nextLine().trim();
                if (option.equals("2")) return false;
            } else {
                readSessionAndRole();
                username = u;
                return true;
            }
        }
    }

    private void upload() throws IOException {
        System.out.println("Enter local file path : ");
        String localPath = input.nextLine().trim();

        Path path = Paths.get(localPath);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            System.out.println("No file with this name/path on your computer.");
            return;
        }

        String filename = path.getFileName().toString();
        long size = Files.size(path);

        out.writeUTF("UPLOAD " + sessionKey + " " + filename);
        out.writeLong(size);

        try (InputStream fileIn = Files.newInputStream(path)) {
            fileIn.transferTo(out);
        }
        out.flush();

        System.out.println("Server: " + in.readUTF());
    }

    private void download() throws IOException {
        System.out.println("Enter server file name : ");
        String filename = input.nextLine().trim();
        if (filename.isEmpty()) return;

        out.writeUTF("DOWNLOAD " + sessionKey + " " + filename);
        out.flush();

        String status = in.readUTF();
        if (!"OK".equals(status)) {
            System.out.println("Server: " + status);
            return;
        }

        long size = in.readLong();
        Path outPath = Paths.get("downloaded_" + filename);

        try (OutputStream fileOut = Files.newOutputStream(outPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            byte[] buffer = new byte[64 * 1024];
            long remaining = size;
            while (remaining > 0) {
                int r = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (r == -1) throw new EOFException("Server ended stream early");
                fileOut.write(buffer, 0, r);
                remaining -= r;
            }
        }

        System.out.println("Downloaded successfully to: " + outPath.toAbsolutePath());
    }

    private void list() throws IOException {
        out.writeUTF("LIST " + sessionKey);
        out.flush();

        String status = in.readUTF();
        if (!"OK".equals(status)) {
            System.out.println("Server: " + status);
            return;
        }

        int count = in.readInt();
        System.out.println("Your files (" + count + "):");
        for (int i = 0; i < count; i++) {
            System.out.println("- " + in.readUTF());
        }
    }

    private void delete() throws IOException {
        System.out.println("Enter server file name : ");
        String filename = input.nextLine().trim();
        if (filename.isEmpty()) return;

        out.writeUTF("DELETE " + sessionKey + " " + filename);
        out.flush();

        System.out.println("Server: " + in.readUTF());
    }

    public void mainClient() {
        try {
            createSocket(host, port);

            boolean ok = login();
            if (!ok) register();

            System.out.println("Logged in as: " + username + " (role: " + role + ")");

            while (true) {
                System.out.println();
                System.out.println("Enter 1 if you need to UPLOAD <filename>");
                System.out.println("Enter 2 if you need to DOWNLOAD <filename>");
                System.out.println("Enter 3 if you need to LIST all files");
                if (role.equals("super"))
                    System.out.println("Enter 4 if you need to DELETE <filename>");
                System.out.println("if you need to leave enter anything else");

                String option = input.nextLine().trim();

                if (option.equals("1")) upload();
                else if (option.equals("2")) download();
                else if (option.equals("3")) list();
                else if (role.equals("super")&&option.equals("4")) delete();
                else break;
            }

        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        } finally {
            try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        }
    }
}
