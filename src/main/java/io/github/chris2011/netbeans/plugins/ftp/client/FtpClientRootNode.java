package io.github.chris2011.netbeans.plugins.ftp.client;

import java.awt.Image;
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
import org.openide.util.ImageUtilities;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = Node.class, path = "Services", position = 100)
@ServicesTabNodeRegistration(
    name = "FtpClientRootNode",
    displayName = "FTP Client",
    shortDescription = "See all FTP Client connections",
    iconResource = "io/github/chris2011/netbeans/plugins/ftp/client/ftp_client.svg",
    position = 2050)
public class FtpClientRootNode extends AbstractNode {

    public FtpClientRootNode() {
        super(Children.LEAF);
        setName("FTP Client");
        setDisplayName("FTP Client");
        setIconBaseWithExtension("io/github/chris2011/netbeans/plugins/ftp/client/ftp_client.svg");

        // Initialize children after construction
        javax.swing.SwingUtilities.invokeLater(() -> {
            setChildren(Children.create(new FtpConnectionChildFactory(), true));
        });
    }

    @Override
    public Image getOpenedIcon(int type) {
        return ImageUtilities.loadImage("io/github/chris2011/netbeans/plugins/ftp/client/ftp_client.svg");
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
            System.out.println("FtpConnectionChildFactory: Constructor called");
            FtpConnectionManager manager = FtpConnectionManager.getInstance();
            manager.addPropertyChangeListener(this);
            System.out.println("FtpConnectionChildFactory: Property listener added, manager has " + manager.getConnections().size() + " connections");
        }

        @Override
        protected boolean createKeys(List<FtpConnection> toPopulate) {
            List<FtpConnection> connections = FtpConnectionManager.getInstance().getConnections();
            System.out.println("FtpConnectionChildFactory.createKeys(): Found " + connections.size() + " connections");
            for (FtpConnection conn : connections) {
                System.out.println("  - Connection: " + conn.getName() + " (" + conn.getId() + ")");
            }
            toPopulate.addAll(connections);
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
            } else if (FtpConnectionManager.PROP_CONNECTION_STATE_CHANGED.equals(evt.getPropertyName())) {
                // Connection state changed - refresh to update node display
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

                openFtpExplorer(connection);
            }
        }

        private void openFtpExplorer(FtpConnection connection) {
            FtpExplorerTopComponent.openForConnection(connection);
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }
}
