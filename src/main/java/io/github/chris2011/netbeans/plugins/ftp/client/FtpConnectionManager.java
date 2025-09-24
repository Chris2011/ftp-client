package io.github.chris2011.netbeans.plugins.ftp.client;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import org.openide.awt.NotificationDisplayer;

public class FtpConnectionManager {

    public static final String PROP_CONNECTIONS_CHANGED = "connectionsChanged";
    public static final String PROP_CONNECTION_STATE_CHANGED = "connectionStateChanged";

    private static FtpConnectionManager instance;
    private final List<FtpConnection> connections;
    private final PropertyChangeSupport pcs;
    private final Path configFile;

    private FtpConnectionManager() {
        this.connections = new ArrayList<>();
        this.pcs = new PropertyChangeSupport(this);

        // Create config directory in user home/.netbeans/ftp-client/
        String userHome = System.getProperty("user.home");
        Path configDir = Paths.get(userHome, ".netbeans", "ftp-client");
        this.configFile = configDir.resolve("connections.json");

        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            System.err.println("Failed to create config directory: " + e.getMessage());
        }

        loadConnections();
    }

    public static synchronized FtpConnectionManager getInstance() {
        if (instance == null) {
            instance = new FtpConnectionManager();
        }
        return instance;
    }

    public void addConnection(FtpConnection connection) {
        connection.setConnected(false);
        connections.add(connection);
        saveConnections();
        pcs.firePropertyChange(PROP_CONNECTIONS_CHANGED, null, connection);
    }

    public void removeConnection(FtpConnection connection) {
        connection.setConnected(false);

        if (connections.remove(connection)) {
            saveConnections();
            pcs.firePropertyChange(PROP_CONNECTIONS_CHANGED, connection, null);
        }
    }

    public void updateConnection(FtpConnection connection, String previousName) {
        int index = connections.indexOf(connection);
        if (index >= 0) {
            connections.set(index, connection);
            saveConnections();
            pcs.firePropertyChange(PROP_CONNECTIONS_CHANGED, connection, connection);
        }
    }

    public List<FtpConnection> getConnections() {
        return Collections.unmodifiableList(connections);
    }

    public void fireConnectionsChanged() {
        pcs.firePropertyChange(PROP_CONNECTIONS_CHANGED, null, connections);
    }

    public void fireConnectionStateChanged(FtpConnection connection) {
        pcs.firePropertyChange(PROP_CONNECTION_STATE_CHANGED, null, connection);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    private void loadConnections() {
        if (!Files.exists(configFile)) {
            return; // No config file yet
        }

        try (BufferedReader reader = Files.newBufferedReader(configFile)) {
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }

            if (json.length() > 0) {
                parseConnections(json.toString());
                // Fire property change to notify UI about loaded connections
                System.out.println("Loaded " + connections.size() + " connections from file");
                pcs.firePropertyChange(PROP_CONNECTIONS_CHANGED, null, connections);
            }
        } catch (IOException e) {
            System.err.println("Failed to load connections: " + e.getMessage());
        }
    }

    private void saveConnections() {
        try (BufferedWriter writer = Files.newBufferedWriter(configFile)) {
            String json = connectionsToJson();
            writer.write(json);
        } catch (IOException e) {
            String errorMessage = "Failed to save connections: " + e.getMessage();
            System.err.println(errorMessage);

            NotificationDisplayer.getDefault().notify(
                "Connection Save Error",
                FtpIcons.getNotificationIcon(),
                "Failed to save FTP connections to file",
                new javax.swing.AbstractAction("Details") {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    javax.swing.JOptionPane.showMessageDialog(
                        null,
                        errorMessage,
                        "Connection Save Error Details",
                        javax.swing.JOptionPane.ERROR_MESSAGE
                    );
                }
            },
                NotificationDisplayer.Priority.HIGH
            );
        }
    }

    private String connectionsToJson() {
        StringBuilder json = new StringBuilder();
        json.append("[\n");

        for (int i = 0; i < connections.size(); i++) {
            FtpConnection conn = connections.get(i);
            if (i > 0) {
                json.append(",\n");
            }

            json.append("  {\n");
            json.append("    \"id\": \"").append(escapeJson(conn.getId())).append("\",\n");
            json.append("    \"name\": \"").append(escapeJson(conn.getName())).append("\",\n");
            json.append("    \"host\": \"").append(escapeJson(conn.getHost())).append("\",\n");
            json.append("    \"port\": ").append(conn.getPort()).append(",\n");
            json.append("    \"username\": \"").append(escapeJson(conn.getUsername())).append("\",\n");

            // Create salted hash for password
            String salt = generateSalt();
            String passwordHash = hashPassword(conn.getPassword(), salt);
            json.append("    \"passwordHash\": \"").append(passwordHash).append("\",\n");
            json.append("    \"salt\": \"").append(salt).append("\",\n");

            json.append("    \"passiveMode\": ").append(conn.isPassiveMode()).append("\n");
            json.append("  }");
        }

        json.append("\n]");
        return json.toString();
    }

    private void parseConnections(String json) {
        // Simple JSON parsing - extract connection objects
        String[] parts = json.split("\\{");
        for (String part : parts) {
            if (part.contains("\"id\"")) {
                try {
                    String id = extractJsonValue(part, "id");
                    String name = extractJsonValue(part, "name");
                    String host = extractJsonValue(part, "host");
                    int port = Integer.parseInt(extractJsonValue(part, "port"));
                    String username = extractJsonValue(part, "username");
                    String passwordHash = extractJsonValue(part, "passwordHash");
                    String salt = extractJsonValue(part, "salt");
                    boolean passiveMode = Boolean.parseBoolean(extractJsonValue(part, "passiveMode"));

                    if (!host.isEmpty() && !username.isEmpty()) {
                        // Create connection without password - will be set when user connects
                        FtpConnection conn = new FtpConnection(id, name, host, port, username, "");
                        conn.setPassiveMode(passiveMode);
                        conn.setConnected(false);

                        // Store hash and salt for later verification
                        conn.setPasswordHash(passwordHash);
                        conn.setSalt(salt);

                        connections.add(conn);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Failed to parse connection: " + e.getMessage());
                }
            }
        }
    }

    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*";
        int start = json.indexOf(pattern);
        if (start == -1) {
            return "";
        }

        start += pattern.length();
        if (json.charAt(start) == '"') {
            // String value
            start++;
            int end = json.indexOf('"', start);
            if (end == -1) {
                return "";
            }
            return json.substring(start, end);
        } else {
            // Number or boolean value
            int end = start;
            while (end < json.length()
                && (Character.isDigit(json.charAt(end))
                || json.charAt(end) == '.'
                || json.charAt(end) == '-'
                || Character.isLetter(json.charAt(end)))) {
                end++;
            }
            return json.substring(start, end).replaceAll("[,\\s}].*", "");
        }
    }

    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(Base64.getDecoder().decode(salt));
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public boolean verifyPassword(FtpConnection connection, String enteredPassword) {
        String expectedHash = connection.getPasswordHash();
        String salt = connection.getSalt();

        if (expectedHash == null || salt == null) {
            return false;
        }

        String enteredHash = hashPassword(enteredPassword, salt);
        return expectedHash.equals(enteredHash);
    }
}
