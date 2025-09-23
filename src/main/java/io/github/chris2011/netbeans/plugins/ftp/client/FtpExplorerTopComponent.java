package io.github.chris2011.netbeans.plugins.ftp.client;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.io.IOException;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.awt.NotificationDisplayer;
import org.openide.awt.StatusDisplayer;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.util.RequestProcessor;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

public class FtpExplorerTopComponent extends TopComponent implements ExplorerManager.Provider {

    private static final String MILLER_VIEW = "miller";
    private static final String TREE_VIEW = "tree";

    private static final RequestProcessor RP = new RequestProcessor(FtpExplorerTopComponent.class);

    private final FtpConnection connection;
    private FtpClient ftpClient;

    private final ExplorerManager explorerManager = new ExplorerManager();

    private JPanel viewPanel;
    private CardLayout cardLayout;
    private MillerColumnsPanel millerPanel;
    private FtpTreePanel treePanel;

    private JToggleButton millerViewButton;
    private JToggleButton treeViewButton;
    private JButton connectButton;
    private JButton disconnectButton;

    private boolean isConnected = false;

    public FtpExplorerTopComponent(FtpConnection connection) {
        this.connection = connection;
        this.ftpClient = new FtpClient(connection);

        setName(connection.getName());
        setDisplayName("FTP: " + connection.getDisplayName());
        setToolTipText("FTP Explorer for " + connection.getDisplayName());

        initComponents();
        associateLookup(ExplorerUtils.createLookup(explorerManager, getActionMap()));
        updateWindowMetadata();
        connectToServer();
    }

    public static TopComponent openForConnection(FtpConnection connection) {
        String tcId = preferredIdFor(connection);

        // Check all open TopComponents for existing instance
        for (TopComponent openTc : WindowManager.getDefault().getRegistry().getOpened()) {
            if (openTc instanceof FtpExplorerTopComponent) {
                FtpExplorerTopComponent ftpTc = (FtpExplorerTopComponent) openTc;
                if (ftpTc.connection.getId().equals(connection.getId())) {
                    // Found existing instance, bring it to front
                    openTc.requestActive();
                    return openTc;
                }
            }
        }

        // No existing instance found, create new one
        TopComponent tc = new FtpExplorerTopComponent(connection);
        tc.open();
        tc.requestActive();
        return tc;
    }

    public static String preferredIdFor(FtpConnection connection) {
        return "FtpClientPlugin.FtpExplorer." + connection.getId();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        connectButton = new JButton("Connect");
        // connectButton.setIcon(ImageUtilities.loadImageIcon("io/github/chris2011/netbeans/plugins/ftp/client/connect.png", false));
        connectButton.addActionListener(e -> connectToServer());

        disconnectButton = new JButton("Disconnect");
        // disconnectButton.setIcon(ImageUtilities.loadImageIcon("io/github/chris2011/netbeans/plugins/ftp/client/disconnect.png", false));
        disconnectButton.addActionListener(e -> disconnectFromServer());
        disconnectButton.setEnabled(false);

        toolbar.add(connectButton);
        toolbar.add(disconnectButton);
        toolbar.addSeparator();

        millerViewButton = new JToggleButton("Miller Columns");
        // millerViewButton.setIcon(ImageUtilities.loadImageIcon("io/github/chris2011/netbeans/plugins/ftp/client/miller.png", false));
        millerViewButton.setSelected(true);
        millerViewButton.addActionListener(e -> switchToMillerView());

        treeViewButton = new JToggleButton("Tree View");
        // treeViewButton.setIcon(ImageUtilities.loadImageIcon("io/github/chris2011/netbeans/plugins/ftp/client/tree.png", false));
        treeViewButton.addActionListener(e -> switchToTreeView());

        toolbar.add(millerViewButton);
        toolbar.add(treeViewButton);

        add(toolbar, BorderLayout.NORTH);

        cardLayout = new CardLayout();
        viewPanel = new JPanel(cardLayout);

        millerPanel = new MillerColumnsPanel(this);
        treePanel = new FtpTreePanel(this, explorerManager);

        viewPanel.add(millerPanel, MILLER_VIEW);
        viewPanel.add(treePanel, TREE_VIEW);

        add(viewPanel, BorderLayout.CENTER);
    }

    private void connectToServer() {
        if (isConnected()) {
            StatusDisplayer.getDefault().setStatusText("Connected to " + connection.getDisplayName());
            return;
        }

        connectButton.setEnabled(false);
        StatusDisplayer.getDefault().setStatusText("Connecting to " + connection.getDisplayName() + "...");

        ProgressHandle handle = ProgressHandleFactory.createHandle(
            "Connecting to " + connection.getDisplayName());
        handle.start();
        handle.switchToIndeterminate();

        RP.post(() -> {
            try {
                boolean connectedNow = ftpClient.connect();
                SwingUtilities.invokeLater(() -> {
                    handle.finish();
                    if (connectedNow) {
                        handleSuccessfulConnection();
                    } else {
                        handleConnectionFailure("Failed to connect to FTP server");
                    }
                });
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    handle.finish();
                    handleConnectionFailure("Connection failed: " + ex.getMessage());
                });
            }
        });
    }

    private void handleSuccessfulConnection() {
        isConnected = true;
        connectButton.setEnabled(false);
        disconnectButton.setEnabled(true);

        refreshCurrentView();

        StatusDisplayer.getDefault().setStatusText("Connected to " + connection.getDisplayName());
        NotificationDisplayer.getDefault().notify(
            "FTP Connection",
            FtpIcons.getNotificationIcon(),
            "Connected to " + connection.getDisplayName(),
            null,
            NotificationDisplayer.Priority.LOW
        );
        connection.setConnected(true);
        updateWindowMetadata();
    }

    private void handleConnectionFailure(String message) {
        isConnected = false;
        connectButton.setEnabled(true);
        disconnectButton.setEnabled(false);

        millerPanel.clear();
        treePanel.clear();

        StatusDisplayer.getDefault().setStatusText(message);
        JOptionPane.showMessageDialog(this, message, "Connection Error", JOptionPane.ERROR_MESSAGE);
    }

    public void updateConnection(FtpConnection updatedConnection) {
        // Copy updated connection details to our connection instance
        connection.setPassword(updatedConnection.getPassword());
        connection.setPasswordHash(updatedConnection.getPasswordHash());
        connection.setSalt(updatedConnection.getSalt());

        // Update the FTP client with new connection details
        this.ftpClient = new FtpClient(connection);
    }

    public void reconnect() {
        // Force disconnect first to ensure clean state
        if (isConnected) {
            ftpClient.disconnect();
            isConnected = false;
            connectButton.setEnabled(true);
            disconnectButton.setEnabled(false);
            connection.setConnected(false);
        }

        // Clear views
        millerPanel.clear();
        treePanel.clear();

        // Now attempt to reconnect
        connectToServer();
    }

    private void disconnectFromServer() {
        ftpClient.disconnect();
        isConnected = false;
        connectButton.setEnabled(true);
        disconnectButton.setEnabled(false);

        millerPanel.clear();
        treePanel.clear();

        StatusDisplayer.getDefault().setStatusText("Disconnected from " + connection.getDisplayName());
        connection.setConnected(false);
        updateWindowMetadata();
    }

    private void switchToMillerView() {
        millerViewButton.setSelected(true);
        treeViewButton.setSelected(false);
        cardLayout.show(viewPanel, MILLER_VIEW);
        if (isConnected) {
            millerPanel.refresh();
        }
    }

    private void switchToTreeView() {
        millerViewButton.setSelected(false);
        treeViewButton.setSelected(true);
        cardLayout.show(viewPanel, TREE_VIEW);
        if (isConnected) {
            treePanel.refresh();
        }
    }

    private void refreshCurrentView() {
        if (millerViewButton.isSelected()) {
            millerPanel.refresh();
        } else {
            treePanel.refresh();
        }
    }

    public List<FtpFile> listFiles(String path) throws IOException {
        return ftpClient.listFiles(path);
    }

    public boolean isConnected() {
        return isConnected && ftpClient.isConnected();
    }

    public FtpClient getFtpClient() {
        return ftpClient;
    }

    public FtpConnection getConnection() {
        return connection;
    }

    void refreshConnectionMetadata() {
        updateWindowMetadata();
    }

    @Override
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_NEVER;
    }

    @Override
    protected void componentClosed() {
        super.componentClosed();
        disconnectFromServer();
    }

    @Override
    public String preferredID() {
        return preferredIdFor(connection);
    }

    @Override
    public ExplorerManager getExplorerManager() {
        return explorerManager;
    }

    private void updateWindowMetadata() {
        setName(connection.getName());
        String baseDisplay = "FTP: " + connection.getDisplayName();
        setDisplayName(baseDisplay);
        setToolTipText("FTP Explorer for " + connection.getDisplayName());

        if (connection.isConnected()) {
            setHtmlDisplayName("<html>" + baseDisplay + " <font color='#388E3C'>(connected)</font></html>");
        } else {
            setHtmlDisplayName(baseDisplay);
        }
    }
}
