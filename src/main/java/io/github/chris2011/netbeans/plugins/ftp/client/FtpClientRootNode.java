package io.github.chris2011.netbeans.plugins.ftp.client;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.api.core.ide.ServicesTabNodeRegistration;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = Node.class, path = "Services", position = 100)
@ServicesTabNodeRegistration(
        name = "FtpClientRootNode",
        displayName = "FTP Client",
        shortDescription = "See all FTP Client connections",
        iconResource="",
//        iconResource = "org/chrisle/netbeans/modules/gitrepoviewer/resources/world.png",
        position = 2021)
public class FtpClientRootNode extends AbstractNode {
    
    public FtpClientRootNode() {
        super(Children.create(new FtpConnectionChildFactory(), true), 
              Lookups.singleton(FtpConnectionManager.getInstance()));
        setName("FTP Client");
        setDisplayName("FTP Client");
        // setIconBaseWithExtension("io/github/chris2011/netbeans/plugins/ftp/client/ftp.png");
    }
    
    @Override
    public Action[] getActions(boolean context) {
        return new Action[]{
            new AddConnectionAction()
        };
    }
    
    private static class FtpConnectionChildFactory extends ChildFactory<FtpConnection> 
            implements PropertyChangeListener {
        
        public FtpConnectionChildFactory() {
            FtpConnectionManager.getInstance().addPropertyChangeListener(this);
        }
        
        @Override
        protected boolean createKeys(List<FtpConnection> toPopulate) {
            toPopulate.addAll(FtpConnectionManager.getInstance().getConnections());
            return true;
        }
        
        @Override
        protected Node createNodeForKey(FtpConnection connection) {
            return new FtpConnectionNode(connection);
        }
        
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (FtpConnectionManager.PROP_CONNECTIONS_CHANGED.equals(evt.getPropertyName())) {
                refresh(false);
            }
        }
    }
    
    private static class AddConnectionAction extends AbstractAction {
        
        public AddConnectionAction() {
            putValue(NAME, "Add Connection...");
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            FtpConnectionDialog dialog = new FtpConnectionDialog();
            if (dialog.showDialog()) {
                FtpConnection connection = dialog.getConnection();
                FtpConnectionManager.getInstance().addConnection(connection);
                
                // Auto-connect with progress bar
                connectWithProgress(connection);
            }
        }
        
        private void connectWithProgress(FtpConnection connection) {
            org.netbeans.api.progress.ProgressHandle handle = 
                org.netbeans.api.progress.ProgressHandleFactory.createHandle("Connecting to " + connection.getDisplayName());
            
            handle.start();
            handle.progress("Establishing FTP connection...");
            
            new Thread(() -> {
                try {
                    FtpClient client = new FtpClient(connection);
                    if (client.connect()) {
                        client.disconnect(); // Just test the connection
                        
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            handle.finish();
                            
                            // Show success notification
                            org.openide.awt.NotificationDisplayer.getDefault().notify(
                                "FTP Connection",
                                null,
                                "Successfully connected to " + connection.getDisplayName() + ". Opening FTP Explorer...",
                                null
                            );
                            
                            // Automatically open TopComponent
                            openFtpExplorer(connection);
                        });
                    } else {
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            handle.finish();
                            org.openide.awt.NotificationDisplayer.getDefault().notify(
                                "FTP Connection Failed",
                                null,
                                "Failed to connect to " + connection.getDisplayName(),
                                null
                            );
                        });
                    }
                } catch (Exception ex) {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        handle.finish();
                        org.openide.awt.NotificationDisplayer.getDefault().notify(
                            "FTP Connection Error",
                            null,
                            "Connection error: " + ex.getMessage(),
                            null
                        );
                    });
                }
            }).start();
        }
        
        private void openFtpExplorer(FtpConnection connection) {
            String tcId = "FtpClientPlugin.FtpExplorer." + connection.getName().replaceAll("[^a-zA-Z0-9]", "_");
            org.openide.windows.TopComponent tc = org.openide.windows.WindowManager.getDefault().findTopComponent(tcId);
            
            if (tc == null) {
                tc = new FtpExplorerTopComponent(connection);
            }
            
            tc.open();
            tc.requestActive();
        }
    }
}