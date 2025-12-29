//import java.io.*;
//import java.net.Socket;
//import java.nio.file.Files;
//import java.util.*;
//
//public class Server extends Thread {
//
//    private Socket client;
//    private PrintWriter write;
//    private BufferedReader read ;
//
//     private Map<String,User> users;
//     private Map<String, List<String>> files;
//
//
//    public Server(Socket accept) throws IOException {
//        client=accept;
//        write = new PrintWriter(this.client.getOutputStream());
//        read = new BufferedReader(new InputStreamReader(this.client.getInputStream()));
//        users=new HashMap<>();
//        files=new HashMap<>();
//    }
//
//
//    public void loadUsersFromFile(String pathFile) throws Exception {
//        Properties props=new Properties();
//
//        try{
//
//            FileInputStream file=new FileInputStream(pathFile);
//            props.load(file);
//        }catch (FileNotFoundException e){
//            throw new Exception("No file with this path ......");
//        }
//
//        for (String username :props.stringPropertyNames() ){
//            String value=props.getProperty(username).trim();
//
//            String []userinfo=value.split(",");
//            if (userinfo.length!=2)continue;
//            users.put(username, new User(username,userinfo[0].trim(),userinfo[1].trim() ));
//        }
//
//    }
//    public void loadUsersFilesFromFile(String pathFile) throws Exception {
//        Properties props=new Properties();
//
//        try{
//            FileInputStream file=new FileInputStream(pathFile);
//            props.load(file);
//        }catch (FileNotFoundException e){
//            throw new Exception("No file with this path ......");
//        }
//        for (String username :props.stringPropertyNames() ){
//            String value=props.getProperty(username).trim();
//
//            String []filesList=value.split(",");
//            files.put(username,List.of(filesList));
//        }
//
//    }
//    public void addNewUser(User user){
//        users.put(user.getUsername(), new User(user.getUsername(),user.getPassword(),user.getRole()));
//    }
//    public boolean checkUserExists(String username){
//        return users.containsKey(username);
//    }
//    public boolean checkUserFilesExists(String username){
//
//        return !files.get(username).isEmpty();
//    }
//    private void login(String username,String password) throws Exception {
//
//        if(!checkUserExists(username))throw new Exception("No user with this username.....");
//        if (!users.get(username).getPassword().equals(password))throw new Exception("Wrong password.....");
//        //you should send a sesstion key to the client here(read the docs)
//        System.out.println("The user "+username+" login in the system ....");
//
//
//    }
//    private void register(String username,String password,String role){
//        users.put(username,new User(username,password,role));
//        //you should send a sesstion key to the client here(read the docs)
//        //also you should save the new user in the users file
//
//    }
//    public void authenticate()throws IOException  {
//        //            while (true) {
//        String command = read.readLine();
//        if (command.equals("login")) {
//            String credentials = read.readLine();
//            String[] parts = credentials.split(",");
//            try {
//                login(parts[0], parts[1]);
//                write.write("login success\n");
//                write.flush();
//            } catch (Exception e) {
//                System.out.println(e.getMessage());
//                write.write(e.getMessage() + "\n");
//                write.flush();
//            }
//        } else if (command.equals("register")) {
//            String credentials = read.readLine();
//            String[] parts = credentials.split(",");
//            register(parts[0], parts[1], parts[2]);
//            write.write("register success\n");
//            write.flush();
//        }
////            }
//    }
//
//    public String uploadFile(String filename){
//        //code to upload file
//        return "file uploaded successfully";
//    }
//    public String downloadFile(String filename,boolean isSuperUser){
//
//        //code to download file:
//
//        if (!isSuperUser){
//            //check if the file belongs to the user
//            //if not return error message
//        }else {
//
//        }
//
//        return "file downloaded successfully";
//    }
//    public String deleteFile(String filename){
//        //code to delete file
//        return "file deleted successfully";
//    }
//    public String listFiles(String username){
//        if(!checkUserFilesExists(username)){
//            return "No files for this user";
//        }
//        List<String> userFiles=files.get(username);
//        StringBuilder sb=new StringBuilder();
//        for (String file:userFiles){
//            sb.append(file).append("\n");
//        }
//        return sb.toString();
//    }
//
//
//    public void run(){
//        try {
//            loadUsersFromFile("users.properties");
//            loadUsersFilesFromFile("filesForUsers.properties");
//            authenticate();
//
//
//
//        } catch (Exception e) {
//            System.out.println(e.getMessage());
//        }
//
//
//
//    }
//}


import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

public class Server extends Thread {

    private Socket client;
    private DataOutputStream out;
    private DataInputStream in;

    private final Map<String, User> users = new HashMap<>();

    private String currentUser = null;
    private String sessionKey = null;
    private int badKeyStreak = 0;

    private static final Path SERVER_DIR = Paths.get("server_files").toAbsolutePath().normalize();
    private static final String USERS_FILE = "users.properties";

    public Server(Socket accept) throws IOException {
        client = accept;
        in = new DataInputStream(new BufferedInputStream(client.getInputStream()));
        out = new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
    }

    private void log(String msg) {
        System.out.println("[" + LocalDateTime.now() + "][" + client.getRemoteSocketAddress() + "] " + msg);
    }

    private void sendError(String message) throws IOException {
        out.writeUTF("ERR " + message);
        out.flush();
    }

    private void sendOk() throws IOException {
        out.writeUTF("OK");
        out.flush();
    }

    private void loadUsersFromFile(String pathFile) throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(pathFile)) {
            props.load(fis);
        } catch (FileNotFoundException e) {
            return;
        }

        for (String username : props.stringPropertyNames()) {
            String value = props.getProperty(username).trim();
            String[] parts = value.split(",", 2);
            if (parts.length != 2) continue;
            users.put(username, new User(username, parts[0].trim(), parts[1].trim()));
        }
    }

    private synchronized void saveUserToFile(User u) throws IOException {
        Properties props = new Properties();
        File f = new File(USERS_FILE);

        if (f.exists()) {
            try (FileInputStream fis = new FileInputStream(f)) {
                props.load(fis);
            }
        }

        props.setProperty(u.getUsername(), u.getPassword() + "," + u.getRole());

        try (FileOutputStream fos = new FileOutputStream(f)) {
            props.store(fos, "ZT-FTP Users");
        }
    }

    private void ensureUserFolder(String username) throws IOException {
        Path userDir = SERVER_DIR.resolve(username).normalize();
        Files.createDirectories(userDir);
    }

    private Path safeUserPath(String username, String filename) throws IOException {
        Path userDir = SERVER_DIR.resolve(username).normalize();
        Path target = userDir.resolve(filename).normalize();
        if (!target.startsWith(userDir)) throw new IOException("Invalid filename");
        return target;
    }

    private void login(String username, String password) throws IOException {
        User u = users.get(username);
        if (u == null) throw new IOException("No user with this username");
        if (!u.getPassword().equals(password)) throw new IOException("Wrong password");

        currentUser = username;
        sessionKey = UUID.randomUUID().toString();
        badKeyStreak = 0;

        ensureUserFolder(username);
        log("LOGIN SUCCESS username=" + username + " role=" + u.getRole());
    }

    private void register(String username, String password, String role) throws IOException {
        if (users.containsKey(username)) throw new IOException("Username already exists");
        if (!role.equalsIgnoreCase("normal") && !role.equalsIgnoreCase("super"))
            throw new IOException("Invalid role");

        User u = new User(username, password, role.toLowerCase());
        users.put(username, u);
        saveUserToFile(u);
        ensureUserFolder(username);

        currentUser = username;
        sessionKey = UUID.randomUUID().toString();
        badKeyStreak = 0;

        log("REGISTER SUCCESS username=" + username + " role=" + role);
    }

    private void authenticate() throws IOException {
        while (currentUser == null) {
            String cmd = in.readUTF();

            if ("LOGIN".equalsIgnoreCase(cmd)) {
                String payload = in.readUTF();
                String[] parts = payload.split(",", 2);
                try {
                    login(parts[0].trim(), parts[1].trim());
                    out.writeUTF("OK LOGIN");
                    out.writeUTF("SESSION " + sessionKey);
                    out.writeUTF("ROLE " + users.get(currentUser).getRole());
                    out.flush();
                } catch (IOException e) {
                    sendError(e.getMessage());
                }

            } else if ("REGISTER".equalsIgnoreCase(cmd)) {
                String payload = in.readUTF();
                String[] parts = payload.split(",", 3);
                try {
                    register(parts[0].trim(), parts[1].trim(), parts[2].trim());
                    out.writeUTF("OK REGISTER");
                    out.writeUTF("SESSION " + sessionKey);
                    out.writeUTF("ROLE " + users.get(currentUser).getRole());
                    out.flush();
                } catch (IOException e) {
                    sendError(e.getMessage());
                }
            } else {
                sendError("Please LOGIN or REGISTER first");
            }
        }
    }

    private boolean verifySessionKey(String providedKey) throws IOException {
        if (sessionKey.equals(providedKey)) {
            badKeyStreak = 0;
            return true;
        }

        badKeyStreak++;
        log("INVALID SESSION KEY attempt " + badKeyStreak);

        if (badKeyStreak >= 3) {
            out.writeUTF("ERR Invalid session key 3 times. Disconnecting.");
            out.flush();
            client.close();
            return false;
        }

        sendError("Invalid session key");
        return false;
    }

    private void handleUpload(User u, String filename) throws IOException {
        if (filename == null || filename.isBlank()) {
            sendError("Missing filename");
            return;
        }

        long size = in.readLong();
        Path target = safeUserPath(currentUser, filename);

        try (OutputStream fos = Files.newOutputStream(target)) {
            byte[] buffer = new byte[64 * 1024];
            long remaining = size;
            while (remaining > 0) {
                int r = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (r == -1) throw new EOFException();
                fos.write(buffer, 0, r);
                remaining -= r;
            }
        }

        sendOk();
    }

    private void handleDownload(User u, String filename) throws IOException {
        Path filePath = u.getRole().equals("super") ? findFileInAllUsers(filename) : safeUserPath(currentUser, filename);
        if (filePath == null || !Files.exists(filePath)) {
            sendError("Not found");
            return;
        }

        out.writeUTF("OK");
        out.writeLong(Files.size(filePath));
        Files.copy(filePath, out);
        out.flush();
    }

    private void handleList(User u) throws IOException {
        Path userDir = SERVER_DIR.resolve(currentUser).normalize();
        List<String> names = new ArrayList<>();

        if (Files.exists(userDir)) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(userDir)) {
                for (Path p : ds)
                    if (Files.isRegularFile(p)) names.add(p.getFileName().toString());
            }
        }

        out.writeUTF("OK");
        out.writeInt(names.size());
        for (String n : names) out.writeUTF(n);
        out.flush();
    }

    private void handleDelete(User u, String filename) throws IOException {
        if (!u.getRole().equals("super")) {
            sendError("Permission denied");
            return;
        }

        Path target = findFileInAllUsers(filename);
        if (target == null) {
            sendError("Not found");
            return;
        }

        Files.delete(target);
        sendOk();
    }

    private Path findFileInAllUsers(String filename) throws IOException {
        if (!Files.exists(SERVER_DIR)) return null;

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(SERVER_DIR)) {
            for (Path dir : ds) {
                Path candidate = dir.resolve(filename).normalize();
                if (candidate.startsWith(dir) && Files.exists(candidate))
                    return candidate;
            }
        }
        return null;
    }

    @Override
    public void run() {
        try {
            Files.createDirectories(SERVER_DIR);
            loadUsersFromFile(USERS_FILE);
            authenticate();

            while (!client.isClosed()) {
                String line = in.readUTF();
                String[] parts = line.split("\\s+");

                if (parts.length < 2) {
                    sendError("Missing session key");
                    continue;
                }

                if (!verifySessionKey(parts[1])) continue;

                User u = users.get(currentUser);
                switch (parts[0].toUpperCase()) {
                    case "UPLOAD" -> handleUpload(u, parts[2]);
                    case "DOWNLOAD" -> handleDownload(u, parts[2]);
                    case "LIST" -> handleList(u);
                    case "DELETE" -> handleDelete(u, parts[2]);
                    default -> sendError("Invalid command");
                }
            }

        } catch (Exception e) {
            log("ERROR " + e.getMessage());
        } finally {
            try { client.close(); } catch (IOException ignored) {}
        }
    }
}
