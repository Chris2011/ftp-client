package io.github.chris2011.netbeans.plugins.ftp.client;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.util.lookup.Lookups;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

public class FtpConnectionNode extends AbstractNode {
    private final FtpConnection connection;
    
    public FtpConnectionNode(FtpConnection connection) {
        super(Children.LEAF, Lookups.singleton(connection));
        this.connection = connection;
        setName(connection.getName());
        setDisplayName(connection.getDisplayName());
        // setIconBaseWithExtension("io/github/chris2011/netbeans/plugins/ftp/client/connection.png");
    }
    
    @Override
    public Action[] getActions(boolean context) {
        return new Action[]{
            new ConnectAction(),
            null,
            new EditConnectionAction(),
            new RemoveConnectionAction()
        };
    }
    
    private class ConnectAction extends AbstractAction {
        
        public ConnectAction() {
            putValue(NAME, "Connect");
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            String tcId = "FtpClientPlugin.FtpExplorer." + connection.getName().replaceAll("[^a-zA-Z0-9]", "_");
            TopComponent tc = WindowManager.getDefault().findTopComponent(tcId);
            
            if (tc == null) {
                tc = new FtpExplorerTopComponent(connection);
            }
            
            tc.open();
            tc.requestActive();
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
                manager.removeConnection(connection);
                manager.addConnection(updatedConnection);
            }
        }
    }
    
    private class RemoveConnectionAction extends AbstractAction {
        
        public RemoveConnectionAction() {
            putValue(NAME, "Remove Connection");
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            FtpConnectionManager.getInstance().removeConnection(connection);
        }
    }
}