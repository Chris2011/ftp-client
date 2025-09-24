package io.github.chris2011.netbeans.plugins.ftp.client.views;

import io.github.chris2011.netbeans.plugins.ftp.client.FtpExplorerTopComponent;
import io.github.chris2011.netbeans.plugins.ftp.client.FtpFile;
import io.github.chris2011.netbeans.plugins.ftp.client.FtpFileOpener;
import io.github.chris2011.netbeans.plugins.ftp.client.FtpIcons;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

public class FtpTreePanel extends BaseViewPanel {

    private final JTree tree;
    private final JTable table;
    private final DefaultTreeModel treeModel;
    private final FtpFileTableModel tableModel;
    private final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    public FtpTreePanel(FtpExplorerTopComponent parentComponent, org.openide.explorer.ExplorerManager explorerManager) {
        super(parentComponent);
        setLayout(new BorderLayout());

        // Create tree for navigation
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("FTP Root");
        treeModel = new DefaultTreeModel(rootNode);
        tree = new JTree(treeModel);
        tree.setRootVisible(true);
        tree.setCellRenderer(new FtpTreeCellRenderer());

        // Create table for file details
        tableModel = new FtpFileTableModel();
        table = new JTable(tableModel);
        table.setRowHeight(22);
        table.setDefaultRenderer(Object.class, new FtpFileTableCellRenderer());

        // Configure column alignment
        setupColumnAlignment();

        // Add tree selection listener
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                onTreeSelectionChanged();
            }
        });

        // Add double-click to open files
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row >= 0) {
                        FtpFile file = tableModel.getFileAt(row);
                        if (file != null && file.isFile()) {
                            FtpFileOpener.openFile(file, parentComponent.getFtpClient());
                        }
                    }
                }
            }
        });

        // Create split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(new JScrollPane(tree));
        splitPane.setRightComponent(new JScrollPane(table));
        splitPane.setDividerLocation(250);
        splitPane.setResizeWeight(0.3);

        add(splitPane, BorderLayout.CENTER);
        clear();
    }

    public void refresh() {
        if (!isConnected()) {
            return;
        }

        try {
            // Build tree structure
            FtpFile rootFile = FtpFile.createRoot(parentComponent.getConnection().getDisplayName());
            DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(new FtpTreeNodeData(rootFile, "/"));
            loadTreeChildren(rootNode, "/");

            treeModel.setRoot(rootNode);
            tree.expandRow(0); // Expand root

            // Load root directory in table
            List<FtpFile> files = listFiles("/");
            tableModel.setFiles(files);
        } catch (IOException e) {
            e.printStackTrace();
            clear();
        }
    }

    public void clear() {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Disconnected");
        treeModel.setRoot(rootNode);
        tableModel.setFiles(new ArrayList<>());
    }

    private void loadTreeChildren(DefaultMutableTreeNode parentNode, String path) {
        try {
            List<FtpFile> files = listFiles(path);
            for (FtpFile file : files) {
                if (file.isDirectory()) {
                    DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(new FtpTreeNodeData(file, file.getPath()));
                    parentNode.add(childNode);
                    // Add dummy child for lazy loading
                    childNode.add(new DefaultMutableTreeNode("Loading..."));
                }
            }
        } catch (IOException e) {
            // Ignore errors for tree building
        }
    }

    private void onTreeSelectionChanged() {
        TreePath selectionPath = tree.getSelectionPath();
        if (selectionPath != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
            Object userObject = node.getUserObject();

            if (userObject instanceof FtpTreeNodeData) {
                FtpTreeNodeData nodeData = (FtpTreeNodeData) userObject;
                loadDirectoryInTable(nodeData.getPath());

                // Lazy load children if needed
                if (node.getChildCount() == 1 && "Loading...".equals(node.getFirstChild().toString())) {
                    node.removeAllChildren();
                    loadTreeChildren(node, nodeData.getPath());
                    treeModel.nodeStructureChanged(node);
                }
            }
        }
    }

    private void loadDirectoryInTable(String path) {
        try {
            List<FtpFile> files = listFiles(path);
            tableModel.setFiles(files);
        } catch (IOException e) {
            e.printStackTrace();
            tableModel.setFiles(new ArrayList<>());
        }
    }

    private void setupColumnAlignment() {
        // Right-align all columns except the first one (Name)
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);

        // Apply to columns 1-4 (Size, Modified, Permissions, Owner)
        for (int i = 1; i < 5; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
        }
    }

    private class FtpFileTableModel extends AbstractTableModel {
        private final String[] columnNames = {"Name", "Size", "Modified", "Permissions", "Owner"};
        private List<FtpFile> files = new ArrayList<>();

        public void setFiles(List<FtpFile> files) {
            this.files = new ArrayList<>(files);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return files.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex >= files.size()) {
                return "";
            }

            FtpFile file = files.get(rowIndex);
            switch (columnIndex) {
                case 0: // Name
                    return file.getName();
                case 1: // Size
                    return file.isDirectory() ? "" : file.getFormattedSize();
                case 2: // Modified
                    return file.isRoot() ? "" : DATE_FORMATTER.format(file.getLastModified());
                case 3: // Permissions
                    return file.isRoot() ? "" : file.getPermissionsWithOctal();
                case 4: // Owner
                    return file.isRoot() ? "" : file.getOwnerDisplay();
                default:
                    return "";
            }
        }

        public FtpFile getFileAt(int rowIndex) {
            if (rowIndex >= 0 && rowIndex < files.size()) {
                return files.get(rowIndex);
            }
            return null;
        }
    }

    private class FtpFileTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            // Set icon for name column
            if (column == 0) {
                FtpFile file = tableModel.getFileAt(row);
                if (file != null) {
                    if (file.isDirectory()) {
                        setIcon(FtpIcons.getFolderIcon());
                    } else {
                        setIcon(FtpIcons.getFileIconByExtension(file.getName()));
                    }
                }
            } else {
                setIcon(null);
            }

            return this;
        }
    }

    // Helper class to store FtpFile data in tree nodes
    private static class FtpTreeNodeData {
        private final FtpFile file;
        private final String path;

        public FtpTreeNodeData(FtpFile file, String path) {
            this.file = file;
            this.path = path;
        }

        public FtpFile getFile() {
            return file;
        }

        public String getPath() {
            return path;
        }

        @Override
        public String toString() {
            return file.getName();
        }
    }

    // Custom tree cell renderer with icons
    private class FtpTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            if (value instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object userObject = node.getUserObject();

                if (userObject instanceof FtpTreeNodeData) {
                    FtpTreeNodeData nodeData = (FtpTreeNodeData) userObject;
                    FtpFile file = nodeData.getFile();

                    if (file.isDirectory()) {
                        setIcon(FtpIcons.getFolderIcon());
                    } else {
                        setIcon(FtpIcons.getConnectionIcon(isConnected()));
                    }
                } else {
                    // Root or other nodes
                    setIcon(FtpIcons.getConnectionIcon(isConnected()));
                }
            }

            return this;
        }
    }
}
