package io.github.chris2011.netbeans.plugins.ftp.client;

import java.io.Serializable;
import java.util.Objects;

public class FtpConnection implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String name;
    private String host;
    private int port;
    private String username;
    private String password;
    private boolean passiveMode;
    
    public FtpConnection() {
        this.port = 21;
        this.passiveMode = true;
    }
    
    public FtpConnection(String name, String host, int port, String username, String password) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.passiveMode = true;
    }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public boolean isPassiveMode() { return passiveMode; }
    public void setPassiveMode(boolean passiveMode) { this.passiveMode = passiveMode; }
    
    public String getDisplayName() {
        return username + "@" + host + ":" + port;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FtpConnection that = (FtpConnection) o;
        return port == that.port && 
               Objects.equals(host, that.host) && 
               Objects.equals(username, that.username);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(host, port, username);
    }
    
    @Override
    public String toString() {
        return getDisplayName();
    }
}