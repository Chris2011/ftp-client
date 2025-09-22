package io.github.chris2011.netbeans.plugins.ftp.client;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.openide.util.ImageUtilities;

public class MillerColumnsPanel extends JPanel {
    private final FtpExplorerTopComponent parentComponent;
    private final List<JList<FtpFile>> columns;
    private final JPanel columnsContainer;
    private String currentPath = "/";
    
    public MillerColumnsPanel(FtpExplorerTopComponent parent) {
        this.parentComponent = parent;
        this.columns = new ArrayList<>();
        
        setLayout(new BorderLayout());
        
        columnsContainer = new JPanel();
        columnsContainer.setLayout(new BoxLayout(columnsContainer, BoxLayout.X_AXIS));
        
        JScrollPane scrollPane = new JScrollPane(columnsContainer);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        
        add(scrollPane, BorderLayout.CENTER);
    }
    
    public void refresh() {
        if (!parentComponent.isConnected()) {
            return;
        }
        
        clear();
        loadPath("/");
    }
    
    public void clear() {
        columnsContainer.removeAll();
        columns.clear();
        currentPath = "/";
        revalidate();
        repaint();
    }
    
    private void loadPath(String path) {
        SwingUtilities.invokeLater(() -> {
            try {
                List<FtpFile> files = parentComponent.listFiles(path);
                addColumn(files, path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
    
    private void addColumn(List<FtpFile> files, String path) {
        DefaultListModel<FtpFile> model = new DefaultListModel<>();
        
        if (!path.equals("/")) {
            model.addElement(new FtpFile("..", getParentPath(path), true));
        }
        
        files.stream()
              .sorted((a, b) -> {
                  if (a.isDirectory() && !b.isDirectory()) return -1;
                  if (!a.isDirectory() && b.isDirectory()) return 1;
                  return a.getName().compareToIgnoreCase(b.getName());
              })
              .forEach(model::addElement);
        
        JList<FtpFile> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new FtpFileListCellRenderer());
        
        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    FtpFile selectedFile = list.getSelectedValue();
                    if (selectedFile != null && selectedFile.isDirectory()) {
                        onDirectorySelected(selectedFile, list);
                    }
                }
            }
        });
        
        // Add double-click listener for file opening
        list.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    FtpFile selectedFile = list.getSelectedValue();
                    if (selectedFile != null && selectedFile.isFile()) {
                        openFileInEditor(selectedFile);
                    }
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(250, 0));
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        columnsContainer.add(scrollPane);
        columns.add(list);
        
        revalidate();
        repaint();
    }
    
    private void onDirectorySelected(FtpFile directory, JList<FtpFile> sourceList) {
        int columnIndex = columns.indexOf(sourceList);
        
        for (int i = columns.size() - 1; i > columnIndex; i--) {
            columnsContainer.remove(i);
            columns.remove(i);
        }
        
        String newPath = directory.getPath();
        if (directory.getName().equals("..")) {
            newPath = getParentPath(currentPath);
        }
        
        currentPath = newPath;
        loadPath(newPath);
    }
    
    private String getParentPath(String path) {
        if (path.equals("/")) return "/";
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash <= 0) return "/";
        return path.substring(0, lastSlash);
    }
    
    private void openFileInEditor(FtpFile file) {
        FtpFileOpener.openFile(file, parentComponent.getFtpClient());
    }
    
    private static class FtpFileListCellRenderer extends DefaultListCellRenderer {
        // private static final Icon FOLDER_ICON = ImageUtilities.loadImageIcon("io/github/chris2011/netbeans/plugins/ftp/client/folder.png", false);
        // private static final Icon FILE_ICON = ImageUtilities.loadImageIcon("io/github/chris2011/netbeans/plugins/ftp/client/file.png", false);
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof FtpFile) {
                FtpFile file = (FtpFile) value;
                
                // setIcon(file.isDirectory() ? FOLDER_ICON : FILE_ICON);
                
                String displayText = "<html><b>" + file.getName() + "</b>";
                if (file.isFile()) {
                    displayText += "<br><small>" + file.getFormattedSize() + 
                                  " - " + file.getLastModified().format(DATE_FORMATTER) + "</small>";
                }
                displayText += "</html>";
                
                setText(displayText);
            }
            
            return this;
        }
    }
}