import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

public class Server extends Thread {

    private final Socket client;
    private final DataOutputStream out;
    private final DataInputStream in;

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
                    if (parts.length < 2) throw new IOException("Bad login payload");
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
                    if (parts.length < 3) throw new IOException("Bad register payload");
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
        if (sessionKey != null && sessionKey.equals(providedKey)) {
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


    private String requireTargetUser(User requester, String[] parts, int targetIndex) throws IOException {
        if (!requester.getRole().equals("super")) {
            return currentUser;
        }

        if (parts.length <= targetIndex) throw new IOException("Missing target username");
        String target = parts[targetIndex].trim();
        if (target.isEmpty()) throw new IOException("Missing target username");
        if (!users.containsKey(target)) throw new IOException("Target user does not exist");

        ensureUserFolder(target);
        return target;
    }

    private void handleUpload(String targetUser, String filename) throws IOException {
        if (filename == null || filename.isBlank()) {
            sendError("Missing filename");
            return;
        }

        long size = in.readLong();
        if (size < 0) {
            sendError("Invalid file size");
            return;
        }

        Path target = safeUserPath(targetUser, filename);

        try (OutputStream fos = Files.newOutputStream(target,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            byte[] buffer = new byte[64 * 1024];
            long remaining = size;
            while (remaining > 0) {
                int r = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (r == -1) throw new EOFException("Client ended stream early");
                fos.write(buffer, 0, r);
                remaining -= r;
            }
        }

        sendOk();
    }

    private void handleDownload(String targetUser, String filename) throws IOException {
        if (filename == null || filename.isBlank()) {
            sendError("Missing filename");
            return;
        }

        Path filePath = safeUserPath(targetUser, filename);
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            sendError("Not found");
            return;
        }

        out.writeUTF("OK");
        out.writeLong(Files.size(filePath));
        Files.copy(filePath, out);
        out.flush();
    }

    private void handleList(String targetUser) throws IOException {
        Path userDir = SERVER_DIR.resolve(targetUser).normalize();
        List<String> names = new ArrayList<>();

        if (Files.exists(userDir)) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(userDir)) {
                for (Path p : ds) {
                    if (Files.isRegularFile(p)) names.add(p.getFileName().toString());
                }
            }
        }

        out.writeUTF("OK");
        out.writeInt(names.size());
        for (String n : names) out.writeUTF(n);
        out.flush();
    }

    private void handleDelete(User requester, String targetUser, String filename) throws IOException {
        if (!requester.getRole().equals("super")) {
            sendError("Permission denied");
            return;
        }

        if (filename == null || filename.isBlank()) {
            sendError("Missing filename");
            return;
        }

        Path target = safeUserPath(targetUser, filename);
        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            sendError("Not found");
            return;
        }

        Files.delete(target);
        sendOk();
    }

    @Override
    public void run() {
        try {
            Files.createDirectories(SERVER_DIR);
            loadUsersFromFile(USERS_FILE);
            authenticate();

            while (!client.isClosed()) {
                String line = in.readUTF();
                if (line == null) break;

                String[] parts = line.trim().split("\\s+");
                if (parts.length < 2) {
                    sendError("Missing session key");
                    continue;
                }

                String cmd = parts[0].toUpperCase();
                String providedKey = parts[1];

                if (!verifySessionKey(providedKey)) break;

                User requester = users.get(currentUser);
                if (requester == null) {
                    sendError("Session user not found");
                    continue;
                }

                try {
                    switch (cmd) {
                        case "UPLOAD" -> {
                            if (requester.getRole().equals("super")) {
                                if (parts.length < 4) { sendError("Usage: UPLOAD <key> <user> <filename>"); continue; }
                                String targetUser = requireTargetUser(requester, parts, 2);
                                handleUpload(targetUser, parts[3]);
                            } else {
                                if (parts.length < 3) { sendError("Usage: UPLOAD <key> <filename>"); continue; }
                                handleUpload(currentUser, parts[2]);
                            }
                        }

                        case "DOWNLOAD" -> {
                            if (requester.getRole().equals("super")) {
                                if (parts.length < 4) { sendError("Usage: DOWNLOAD <key> <user> <filename>"); continue; }
                                String targetUser = requireTargetUser(requester, parts, 2);
                                handleDownload(targetUser, parts[3]);
                            } else {
                                if (parts.length < 3) { sendError("Usage: DOWNLOAD <key> <filename>"); continue; }
                                handleDownload(currentUser, parts[2]);
                            }
                        }

                        case "LIST" -> {
                            if (requester.getRole().equals("super")) {
                                if (parts.length < 3) { sendError("Usage: LIST <key> <user>"); continue; }
                                String targetUser = requireTargetUser(requester, parts, 2);
                                handleList(targetUser);
                            } else {
                                handleList(currentUser);
                            }
                        }

                        case "DELETE" -> {

                            if (parts.length < 4) { sendError("Usage: DELETE <key> <user> <filename>"); continue; }
                            String targetUser = requireTargetUser(requester, parts, 2);
                            handleDelete(requester, targetUser, parts[3]);
                        }

                        default -> sendError("Invalid command");
                    }
                } catch (IOException ex) {
                    sendError(ex.getMessage());
                }
            }

        } catch (Exception e) {
            log("ERROR " + e.getMessage());
        } finally {
            try { client.close(); } catch (IOException ignored) {}
        }
    }
}
