package io.github.chris2011.netbeans.plugins.ftp.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

public class FtpClient {
    private final FtpConnection connection;
    FTPClient ftpClient; // Package-private for FtpFileObject access
    private boolean connected = false;
    
    public FtpClient(FtpConnection connection) {
        this.connection = connection;
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
            return true;
            
        } catch (IOException e) {
            disconnect();
            throw e;
        }
    }
    
    public void disconnect() {
        if (ftpClient != null && ftpClient.isConnected()) {
            try {
                ftpClient.logout();
                ftpClient.disconnect();
            } catch (IOException e) {
            }
        }
        connected = false;
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
}