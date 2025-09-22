package io.github.chris2011.netbeans.plugins.ftp.client;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import org.openide.util.ImageUtilities;
import org.openide.windows.TopComponent;

public class FtpExplorerTopComponent extends TopComponent {
    private static final String MILLER_VIEW = "miller";
    private static final String TREE_VIEW = "tree";
    
    private final FtpConnection connection;
    private FtpClient ftpClient;
    
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
        connectToServer();
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
        treePanel = new FtpTreePanel(this);
        
        viewPanel.add(millerPanel, MILLER_VIEW);
        viewPanel.add(treePanel, TREE_VIEW);
        
        add(viewPanel, BorderLayout.CENTER);
    }
    
    private void connectToServer() {
        connectButton.setEnabled(false);
        SwingUtilities.invokeLater(() -> {
            try {
                if (ftpClient.connect()) {
                    isConnected = true;
                    connectButton.setEnabled(false);
                    disconnectButton.setEnabled(true);
                    
                    refreshCurrentView();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to connect to FTP server", "Connection Error", JOptionPane.ERROR_MESSAGE);
                    connectButton.setEnabled(true);
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Connection failed: " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
                connectButton.setEnabled(true);
            }
        });
    }
    
    private void disconnectFromServer() {
        ftpClient.disconnect();
        isConnected = false;
        connectButton.setEnabled(true);
        disconnectButton.setEnabled(false);
        
        millerPanel.clear();
        treePanel.clear();
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
        return "FtpClientPlugin.FtpExplorer." + connection.getName().replaceAll("[^a-zA-Z0-9]", "_");
    }
}