package io.github.chris2011.netbeans.plugins.ftp.client;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;

public class FtpConnectionManager {
    public static final String PROP_CONNECTIONS_CHANGED = "connectionsChanged";
    
    private static FtpConnectionManager instance;
    private final List<FtpConnection> connections;
    private final PropertyChangeSupport pcs;
    private final Preferences prefs;
    
    private FtpConnectionManager() {
        this.connections = new ArrayList<>();
        this.pcs = new PropertyChangeSupport(this);
        this.prefs = NbPreferences.forModule(FtpConnectionManager.class);
        loadConnections();
    }
    
    public static synchronized FtpConnectionManager getInstance() {
        if (instance == null) {
            instance = new FtpConnectionManager();
        }
        return instance;
    }
    
    public void addConnection(FtpConnection connection) {
        connections.add(connection);
        saveConnections();
        pcs.firePropertyChange(PROP_CONNECTIONS_CHANGED, null, connection);
    }
    
    public void removeConnection(FtpConnection connection) {
        if (connections.remove(connection)) {
            saveConnections();
            pcs.firePropertyChange(PROP_CONNECTIONS_CHANGED, connection, null);
        }
    }
    
    public List<FtpConnection> getConnections() {
        return Collections.unmodifiableList(connections);
    }
    
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }
    
    private void loadConnections() {
        String[] connectionNames = prefs.get("connectionNames", "").split(";");
        for (String name : connectionNames) {
            if (!name.trim().isEmpty()) {
                String prefix = "connection." + name + ".";
                String host = prefs.get(prefix + "host", "");
                int port = prefs.getInt(prefix + "port", 21);
                String username = prefs.get(prefix + "username", "");
                String password = prefs.get(prefix + "password", "");
                boolean passiveMode = prefs.getBoolean(prefix + "passiveMode", true);
                
                if (!host.isEmpty() && !username.isEmpty()) {
                    FtpConnection conn = new FtpConnection(name, host, port, username, password);
                    conn.setPassiveMode(passiveMode);
                    connections.add(conn);
                }
            }
        }
    }
    
    private void saveConnections() {
        StringBuilder names = new StringBuilder();
        for (FtpConnection conn : connections) {
            if (names.length() > 0) names.append(";");
            names.append(conn.getName());
            
            String prefix = "connection." + conn.getName() + ".";
            prefs.put(prefix + "host", conn.getHost());
            prefs.putInt(prefix + "port", conn.getPort());
            prefs.put(prefix + "username", conn.getUsername());
            prefs.put(prefix + "password", conn.getPassword());
            prefs.putBoolean(prefix + "passiveMode", conn.isPassiveMode());
        }
        prefs.put("connectionNames", names.toString());
    }
}