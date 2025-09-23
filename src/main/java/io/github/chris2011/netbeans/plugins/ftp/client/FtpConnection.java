package io.github.chris2011.netbeans.plugins.ftp.client;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class FtpConnection implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String PROP_CONNECTED = "connected";

    private final String id;

    private String name;
    private String host;
    private int port;
    private String username;
    private String password;
    private boolean passiveMode;

    private transient PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private volatile boolean connected;

    // For password hashing and verification
    private transient String passwordHash;
    private transient String salt;

    public FtpConnection() {
        this((String) null);
    }

    public FtpConnection(String name, String host, int port, String username, String password) {
        this(null, name, host, port, username, password);
    }

    public FtpConnection(String id, String name, String host, int port, String username, String password) {
        this(id);
        this.name = name;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.passiveMode = true;
    }

    public FtpConnection(FtpConnection template) {
        this(template != null ? template.getId() : null);
        if (template != null) {
            this.name = template.getName();
            this.host = template.getHost();
            this.port = template.getPort();
            this.username = template.getUsername();
            this.password = template.getPassword();
            this.passiveMode = template.isPassiveMode();
        }
    }

    private FtpConnection(String id) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.port = 21;
        this.passiveMode = true;
        this.connected = false;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isPassiveMode() {
        return passiveMode;
    }

    public void setPassiveMode(boolean passiveMode) {
        this.passiveMode = passiveMode;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        boolean old = this.connected;
        this.connected = connected;
        pcs.firePropertyChange(PROP_CONNECTED, old, connected);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public String getDisplayName() {
        return username + "@" + host + ":" + port;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public boolean hasStoredPassword() {
        return passwordHash != null && salt != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FtpConnection that = (FtpConnection) o;

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        pcs = new PropertyChangeSupport(this);
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
