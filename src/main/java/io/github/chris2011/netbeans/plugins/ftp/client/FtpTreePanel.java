package io.github.chris2011.netbeans.plugins.ftp.client;

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import org.openide.util.ImageUtilities;

public class FtpTreePanel extends JPanel {
    private final FtpExplorerTopComponent parentComponent;
    private JTree tree;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootNode;
    
    public FtpTreePanel(FtpExplorerTopComponent parent) {
        this.parentComponent = parent;
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        
        rootNode = new DefaultMutableTreeNode(new FtpFile("Root", "/", true));
        treeModel = new DefaultTreeModel(rootNode);
        tree = new JTree(treeModel);
        
        tree.setCellRenderer(new FtpTreeCellRenderer());
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        
        tree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                TreePath path = event.getPath();
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                loadChildren(node);
            }
            
            @Override
            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
            }
        });
        
        // Add double-click listener for file opening
        tree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        Object userObject = node.getUserObject();
                        if (userObject instanceof FtpFile) {
                            FtpFile file = (FtpFile) userObject;
                            if (file.isFile()) {
                                openFileInEditor(file);
                            }
                        }
                    }
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(tree);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    public void refresh() {
        if (!parentComponent.isConnected()) {
            return;
        }
        
        rootNode.removeAllChildren();
        loadChildren(rootNode);
        treeModel.reload();
        
        SwingUtilities.invokeLater(() -> {
            tree.expandPath(new TreePath(rootNode.getPath()));
        });
    }
    
    public void clear() {
        rootNode.removeAllChildren();
        treeModel.reload();
    }
    
    private void loadChildren(DefaultMutableTreeNode parentNode) {
        if (parentNode.getChildCount() > 0) {
            return;
        }
        
        Object userObject = parentNode.getUserObject();
        if (!(userObject instanceof FtpFile)) {
            return;
        }
        
        FtpFile parentFile = (FtpFile) userObject;
        if (!parentFile.isDirectory()) {
            return;
        }
        
        SwingUtilities.invokeLater(() -> {
            try {
                List<FtpFile> files = parentComponent.listFiles(parentFile.getPath());
                
                List<FtpFile> directories = new ArrayList<>();
                List<FtpFile> regularFiles = new ArrayList<>();
                
                for (FtpFile file : files) {
                    if (file.isDirectory()) {
                        directories.add(file);
                    } else {
                        regularFiles.add(file);
                    }
                }
                
                directories.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                regularFiles.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                
                for (FtpFile dir : directories) {
                    DefaultMutableTreeNode dirNode = new DefaultMutableTreeNode(dir);
                    parentNode.add(dirNode);
                }
                
                for (FtpFile file : regularFiles) {
                    DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(file);
                    parentNode.add(fileNode);
                }
                
                treeModel.nodeStructureChanged(parentNode);
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
    
    private void openFileInEditor(FtpFile file) {
        FtpFileOpener.openFile(file, parentComponent.getFtpClient());
    }
    
    private static class FtpTreeCellRenderer extends DefaultTreeCellRenderer {
        // private static final Icon FOLDER_ICON = ImageUtilities.loadImageIcon("io/github/chris2011/netbeans/plugins/ftp/client/folder.png", false);
        // private static final Icon FILE_ICON = ImageUtilities.loadImageIcon("io/github/chris2011/netbeans/plugins/ftp/client/file.png", false);
        // private static final Icon ROOT_ICON = ImageUtilities.loadImageIcon("io/github/chris2011/netbeans/plugins/ftp/client/server.png", false);
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                boolean expanded, boolean leaf, int row, boolean hasFocus) {
            
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            
            if (value instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object userObject = node.getUserObject();
                
                if (userObject instanceof FtpFile) {
                    FtpFile file = (FtpFile) userObject;
                    
                    if (file.getPath().equals("/")) {
                        // setIcon(ROOT_ICON);
                        setText("FTP Root");
                    } else {
                        // setIcon(file.isDirectory() ? FOLDER_ICON : FILE_ICON);
                        
                        String displayText = file.getName();
                        if (file.isFile()) {
                            displayText += " (" + file.getFormattedSize() + ")";
                        }
                        setText(displayText);
                    }
                    
                    setToolTipText(createTooltip(file));
                }
            }
            
            return this;
        }
        
        private String createTooltip(FtpFile file) {
            StringBuilder tooltip = new StringBuilder("<html>");
            tooltip.append("<b>").append(file.getName()).append("</b><br>");
            tooltip.append("Path: ").append(file.getPath()).append("<br>");
            tooltip.append("Type: ").append(file.isDirectory() ? "Directory" : "File").append("<br>");
            
            if (file.isFile()) {
                tooltip.append("Size: ").append(file.getFormattedSize()).append("<br>");
            }
            
            tooltip.append("Modified: ").append(file.getLastModified().format(DATE_FORMATTER)).append("<br>");
            tooltip.append("Permissions: ").append(file.getPermissions());
            tooltip.append("</html>");
            
            return tooltip.toString();
        }
    }
}