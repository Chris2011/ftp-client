package io.github.chris2011.netbeans.plugins.ftp.client;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import org.apache.commons.net.ftp.FTPFile;

public class FtpFile {
    private final String name;
    private final String path;
    private final boolean isDirectory;
    private final long size;
    private final LocalDateTime lastModified;
    private final String permissions;
    
    public FtpFile(String path, FTPFile ftpFile) {
        this.name = ftpFile.getName();
        this.path = path.endsWith("/") ? path + name : path + "/" + name;
        this.isDirectory = ftpFile.isDirectory();
        this.size = ftpFile.getSize();
        this.permissions = ftpFile.toFormattedString().substring(0, 10);
        
        Date timestamp = ftpFile.getTimestamp() != null ? ftpFile.getTimestamp().getTime() : new Date();
        this.lastModified = timestamp.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
    
    public FtpFile(String name, String path, boolean isDirectory) {
        this.name = name;
        this.path = path;
        this.isDirectory = isDirectory;
        this.size = 0;
        this.lastModified = LocalDateTime.now();
        this.permissions = isDirectory ? "drwxr-xr-x" : "-rw-r--r--";
    }
    
    public String getName() { return name; }
    public String getPath() { return path; }
    public boolean isDirectory() { return isDirectory; }
    public boolean isFile() { return !isDirectory; }
    public long getSize() { return size; }
    public LocalDateTime getLastModified() { return lastModified; }
    public String getPermissions() { return permissions; }
    
    public String getParentPath() {
        if (path.equals("/")) return null;
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash <= 0) return "/";
        return path.substring(0, lastSlash);
    }
    
    public String getFormattedSize() {
        if (isDirectory) return "";
        
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024.0));
        return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
    }
    
    @Override
    public String toString() {
        return name;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        FtpFile ftpFile = (FtpFile) obj;
        return path.equals(ftpFile.path);
    }
    
    @Override
    public int hashCode() {
        return path.hashCode();
    }
}