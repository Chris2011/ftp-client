package io.github.chris2011.netbeans.plugins.ftp.client;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

public class FtpClient {

    public static final String PROP_CONNECTED = "connected";
    public static final String PROP_DISCONNECTED = "disconnected";

    private static final Map<String, FtpClient> instances = new HashMap<>();

    private final FtpConnection connection;
    private final PropertyChangeSupport pcs;
    FTPClient ftpClient; // Package-private for FtpFileObject access
    private boolean connected = false;

    private FtpClient(FtpConnection connection) {
        this.connection = connection;
        this.pcs = new PropertyChangeSupport(this);
    }

    public static synchronized FtpClient getInstance(FtpConnection connection) {
        String key = connection.getName(); // Use connection name as key
        FtpClient instance = instances.get(key);

        if (instance == null) {
            instance = new FtpClient(connection);
            instances.put(key, instance);
        } else {
            // Update connection details if they changed
            instance.updateConnection(connection);
        }

        return instance;
    }

    public static synchronized FtpClient findInstance(FtpConnection connection) {
        return instances.get(connection.getName());
    }

    public static synchronized void removeInstance(FtpConnection connection) {
        FtpClient instance = instances.get(connection.getName());
        if (instance != null) {
            instance.disconnect();
            instances.remove(connection.getName());
        }
    }

    private void updateConnection(FtpConnection newConnection) {
        // Only update if not connected or connection details changed
        if (!connected || !this.connection.equals(newConnection)) {
            // Copy updated connection details
            this.connection.setHost(newConnection.getHost());
            this.connection.setPort(newConnection.getPort());
            this.connection.setUsername(newConnection.getUsername());
            this.connection.setPassword(newConnection.getPassword());
            this.connection.setPassiveMode(newConnection.isPassiveMode());
        }
    }

    public boolean connect() throws IOException {
        if (connected) return true;

        ftpClient = new FTPClient();

        try {
            ftpClient.connect(connection.getHost(), connection.getPort());

            if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                throw new IOException("FTP server refused connection.");
            }

            if (!ftpClient.login(connection.getUsername(), connection.getPassword())) {
                throw new IOException("FTP login failed.");
            }

            if (connection.isPassiveMode()) {
                ftpClient.enterLocalPassiveMode();
            } else {
                ftpClient.enterLocalActiveMode();
            }

            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            connected = true;
            connection.setConnected(true);

            // Fire event to notify all listeners
            pcs.firePropertyChange(PROP_CONNECTED, false, true);
            FtpConnectionManager.getInstance().fireConnectionStateChanged(connection);

            return true;

        } catch (IOException e) {
            disconnect();
            throw e;
        }
    }

    public void disconnect() {
        boolean wasConnected = connected;

        if (ftpClient != null && ftpClient.isConnected()) {
            try {
                ftpClient.logout();
                ftpClient.disconnect();
            } catch (IOException e) {
                // Ignore cleanup errors
            }
        }
        connected = false;
        connection.setConnected(false);

        if (wasConnected) {
            // Fire event to notify all listeners
            pcs.firePropertyChange(PROP_DISCONNECTED, true, false);
            FtpConnectionManager.getInstance().fireConnectionStateChanged(connection);
        }
    }

    public boolean isConnected() {
        return connected && ftpClient != null && ftpClient.isConnected();
    }

    public List<FtpFile> listFiles(String path) throws IOException {
        if (!isConnected()) {
            throw new IOException("Not connected to FTP server");
        }

        if (path == null || path.isEmpty()) {
            path = "/";
        }

        try {
            FTPFile[] files = ftpClient.listFiles(path);
            List<FtpFile> result = new ArrayList<>();

            for (FTPFile file : files) {
                if (!file.getName().equals(".") && !file.getName().equals("..")) {
                    result.add(new FtpFile(path, file));
                }
            }

            return result;
        } catch (org.apache.commons.net.ftp.parser.ParserInitializationException e) {
            // Fallback: Use simple file listing for unknown server types like Win32NT
            return listFilesSimple(path);
        }
    }

    private List<FtpFile> listFilesSimple(String path) throws IOException {
        try {
            // Use listNames() as fallback - this gives us just file names
            String[] fileNames = ftpClient.listNames(path);
            if (fileNames == null) {
                return new ArrayList<>();
            }

            List<FtpFile> result = new ArrayList<>();

            for (String fileName : fileNames) {
                if (!fileName.equals(".") && !fileName.equals("..")) {
                    // Simple heuristic: if it has no extension or ends with /, it's probably a directory
                    boolean isDirectory = fileName.endsWith("/") || (!fileName.contains(".") && !fileName.contains(" "));

                    // Clean up the name
                    String cleanName = fileName.endsWith("/") ? fileName.substring(0, fileName.length() - 1) : fileName;
                    String fullPath = path.endsWith("/") ? path + cleanName : path + "/" + cleanName;

                    result.add(new FtpFile(cleanName, fullPath, isDirectory));
                }
            }

            return result;
        } catch (Exception e) {
            // Last resort: return empty list
            return new ArrayList<>();
        }
    }

    public boolean changeDirectory(String path) throws IOException {
        if (!isConnected()) {
            throw new IOException("Not connected to FTP server");
        }

        return ftpClient.changeWorkingDirectory(path);
    }

    public String getCurrentDirectory() throws IOException {
        if (!isConnected()) {
            throw new IOException("Not connected to FTP server");
        }

        return ftpClient.printWorkingDirectory();
    }

    public FtpConnection getConnection() {
        return connection;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }
}