package io.github.chris2011.netbeans.plugins.ftp.client;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.WeakListeners;
import org.openide.util.lookup.Lookups;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

public class FtpConnectionNode extends AbstractNode {

    private final FtpConnection connection;

    private final Action openExplorerAction = new OpenExplorerAction();
    private final Action connectAndOpenAction = new ConnectAndOpenAction();
    private final PropertyChangeListener connectionListener = this::onConnectionPropertyChange;

    private final FtpClient ftpClient;

    public FtpConnectionNode(FtpConnection connection) {
        super(Children.LEAF, Lookups.singleton(connection));
        this.connection = connection;
        setName(connection.getName());
        setDisplayName(connection.getDisplayName());
        connection.addPropertyChangeListener(WeakListeners.propertyChange(connectionListener, connection));
        updatePresentation();

        ftpClient = FtpClient.getInstance(connection);
    }

    @Override
    public Action[] getActions(boolean context) {
        if (connection.isConnected()) {
            return new Action[]{
                openExplorerAction,
                new DisconnectAction(),
                null,
                new EditConnectionAction(),
                new RemoveConnectionAction()
            };
        } else {
            return new Action[]{
                connectAndOpenAction,
                null,
                new EditConnectionAction(),
                new RemoveConnectionAction()
            };
        }
    }

    @Override
    public Action getPreferredAction() {
        return connection.isConnected() ? openExplorerAction : connectAndOpenAction;
    }

    @Override
    public Image getIcon(int type) {
        Image icon = FtpIcons.getConnectionImage(connection.isConnected());
        return icon != null ? icon : super.getIcon(type);
    }

    @Override
    public Image getOpenedIcon(int type) {
        return getIcon(type);
    }

    @Override
    public String getHtmlDisplayName() {
        boolean connected = connection.isConnected();
        String statusColor = connected ? "#388E3C" : "#757575";
        String statusText = connected ? "(connected)" : "(disconnected)";

        return connection.getName() + " <font color='" + statusColor + "'>" + statusText + "</font>";
    }

    private void updatePresentation() {
        boolean connected = connection.isConnected();

        setName(connection.getName());
        setDisplayName(connection.getName()); // Plain text fallback

        // Set tooltip with detailed status
        setShortDescription(connection.getDisplayName() + (connected ? " is connected" : " is disconnected"));

        // Fire events to refresh the display
        fireDisplayNameChange(null, connection.getName());
        fireIconChange();
        fireOpenedIconChange();
        firePropertySetsChange(null, null); // Refresh actions
    }

    private void onConnectionPropertyChange(PropertyChangeEvent event) {
        if (FtpConnection.PROP_CONNECTED.equals(event.getPropertyName())) {
            SwingUtilities.invokeLater(this::updatePresentation);
        }

    }

    private class OpenExplorerAction extends AbstractAction {

        public OpenExplorerAction() {
            putValue(NAME, "Open Explorer");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            FtpExplorerTopComponent.openForConnection(connection);
        }
    }

    private class ConnectAndOpenAction extends AbstractAction {

        public ConnectAndOpenAction() {
            putValue(NAME, "Connect");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Check if this is a saved connection that needs password verification
            if (connection.hasStoredPassword()) {
                // Show password dialog for verification
                FtpConnectionDialog dialog = new FtpConnectionDialog(connection, true); // true = password only mode
                if (dialog.showDialog()) {
                    String enteredPassword = dialog.getEnteredPassword();
                    FtpConnectionManager manager = FtpConnectionManager.getInstance();

                    if (manager.verifyPassword(connection, enteredPassword)) {
                        // Password correct, set it and open explorer
                        connection.setPassword(enteredPassword);
                        openOrReconnectExplorer();
                    } else {
                        // Wrong password
                        javax.swing.JOptionPane.showMessageDialog(
                            null,
                            "Incorrect password for connection: " + connection.getName(),
                            "Authentication Failed",
                            javax.swing.JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            } else {
                // New connection or no stored password, open explorer directly
                openOrReconnectExplorer();
            }
        }

        private void openOrReconnectExplorer() {
            // Check if there's already an open window for this connection
            TopComponent existingTc = WindowManager.getDefault()
                .findTopComponent(FtpExplorerTopComponent.preferredIdFor(connection));

            if (existingTc instanceof FtpExplorerTopComponent) {
                // Window exists, bring it to front
                existingTc.requestActive();
            } else {
                // No existing window, open new one
                FtpExplorerTopComponent.openForConnection(connection);
            }

            // Always connect via singleton - events will handle UI updates in all open windows
            FtpClient client = FtpClient.getInstance(connection);
            SwingUtilities.invokeLater(() -> {
                try {
                    client.connect();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null,
                        "Connection failed: " + ex.getMessage(),
                        "Connection Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }

    private class EditConnectionAction extends AbstractAction {

        public EditConnectionAction() {
            putValue(NAME, "Edit Connection...");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            FtpConnectionDialog dialog = new FtpConnectionDialog(connection);
            if (dialog.showDialog()) {
                FtpConnection updatedConnection = dialog.getConnection();
                FtpConnectionManager manager = FtpConnectionManager.getInstance();
                if (updatedConnection != null) {
                    String previousName = connection.getName();
                    applyUpdates(updatedConnection);
                    manager.updateConnection(connection, previousName);
                    TopComponent tc = WindowManager.getDefault()
                        .findTopComponent(FtpExplorerTopComponent.preferredIdFor(connection));
                    if (tc instanceof FtpExplorerTopComponent) {
                        ((FtpExplorerTopComponent) tc).refreshConnectionMetadata();
                    }
                    updatePresentation();
                }
            }
        }

        private void applyUpdates(FtpConnection updatedConnection) {
            connection.setName(updatedConnection.getName());
            connection.setHost(updatedConnection.getHost());
            connection.setPort(updatedConnection.getPort());
            connection.setUsername(updatedConnection.getUsername());
            connection.setPassword(updatedConnection.getPassword());
            connection.setPassiveMode(updatedConnection.isPassiveMode());
        }

    }

    private class DisconnectAction extends AbstractAction {

        public DisconnectAction() {
            putValue(NAME, "Disconnect");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (connection.isConnected()) {
                // Simply disconnect via singleton client - events will handle UI updates
                FtpClient.getInstance(connection).disconnect();
            }
        }
    }

    private class RemoveConnectionAction extends AbstractAction {

        public RemoveConnectionAction() {
            putValue(NAME, "Remove Connection");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            TopComponent tc = WindowManager.getDefault().findTopComponent(FtpExplorerTopComponent.preferredIdFor(connection));
            if (tc instanceof FtpExplorerTopComponent) {
                tc.close();
            }

            // Remove singleton instance
            FtpClient.removeInstance(connection);
            FtpConnectionManager.getInstance().removeConnection(connection);
        }
    }
}
