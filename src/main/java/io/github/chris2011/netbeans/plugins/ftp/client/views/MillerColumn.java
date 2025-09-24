package io.github.chris2011.netbeans.plugins.ftp.client.views;

import io.github.chris2011.netbeans.plugins.ftp.client.FtpFile;
import io.github.chris2011.netbeans.plugins.ftp.client.FtpFileOpener;
import io.github.chris2011.netbeans.plugins.ftp.client.FtpIcons;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelListener;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

public class MillerColumn extends JScrollPane {

    public interface ColumnListener {

        void onDirectorySelected(FtpFile directory, MillerColumn sourceColumn);
    }

    private final JList<FtpFile> list;
    private final String path;
    private final ColumnListener listener;

    public MillerColumn(List<FtpFile> files, String path, ColumnListener listener,
        MouseWheelListener horizontalScrollListener, KeyListener keyListener) {
        this.path = path;
        this.listener = listener;

        DefaultListModel<FtpFile> model = new DefaultListModel<>();
        files.stream()
            .sorted((a, b) -> {
                if (a.isDirectory() && !b.isDirectory()) {
                    return -1;
                }
                if (!a.isDirectory() && b.isDirectory()) {
                    return 1;
                }
                return a.getName().compareToIgnoreCase(b.getName());
            })
            .forEach(model::addElement);

        list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new FtpFileListCellRenderer());

        // Add keyboard navigation
        list.addKeyListener(keyListener);

        // Add double-click listener for file/directory opening
        list.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    FtpFile selectedFile = list.getSelectedValue();
                    if (selectedFile != null) {
                        if (selectedFile.isDirectory()) {
                            listener.onDirectorySelected(selectedFile, MillerColumn.this);
                        } else {
                            openFileInEditor(selectedFile);
                        }
                    }
                }
            }
        });

        setViewportView(list);
        addMouseWheelListener(horizontalScrollListener);

        setPreferredSize(new Dimension(220, 0));
        setMinimumSize(new Dimension(220, 0));
        setMaximumSize(new Dimension(220, Integer.MAX_VALUE));
        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Improve vertical scrolling
        getVerticalScrollBar().setUnitIncrement(16);
        getVerticalScrollBar().setBlockIncrement(50);
    }

    public JList<FtpFile> getList() {
        return list;
    }

    public String getPath() {
        return path;
    }

    @Override
    public void requestFocus() {
        list.requestFocusInWindow();
        if (list.getSelectedIndex() == -1 && list.getModel().getSize() > 0) {
            list.setSelectedIndex(0);
        }
    }

    private void openFileInEditor(FtpFile file) {
        // Need to pass FtpClient or use callback to parent
        if (listener instanceof MillerColumnsPanel) {
            MillerColumnsPanel panel = (MillerColumnsPanel) listener;
            panel.openFileInEditor(file);
        }
    }

    private static class FtpFileListCellRenderer extends DefaultListCellRenderer {

        private static final Icon FOLDER_ICON = FtpIcons.getFolderIcon();
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        private static final int DIRECTORY_ARROW_SIZE = 8;
        private static final int DIRECTORY_ARROW_GAP = 6;

        private boolean isDirectory = false;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {

            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof FtpFile) {
                FtpFile file = (FtpFile) value;
                this.isDirectory = file.isDirectory();

                // Use appropriate icon based on file type
                if (file.isDirectory()) {
                    setIcon(FOLDER_ICON);
                } else {
                    setIcon(FtpIcons.getFileIconByExtension(file.getName()));
                }

                // Create display text
                String fileName = file.getName();

                if (file.isFile()) {
                    String displayText = "<html><b>" + fileName + "</b><br />"
                        + "<small>" + file.getFormattedSize() + " - " + file.getLastModified().format(DATE_FORMATTER) + "</small>"
                        + "</div></html>";
                    setText(displayText);
                    setToolTipText(fileName);
                } else {
                    String displayText = "<html><span style='width: 160px; overflow: hidden; white-space: nowrap; text-overflow: ellipsis;'>"
                        + fileName + "</span></html>";
                    setText(displayText);
                    setToolTipText(fileName);
                }

                int rightPadding = isDirectory ? 25 : 8;
                setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, rightPadding));
            }

            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (isDirectory) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth();
                int height = getHeight();
                int centerY = height / 2;

                // Position arrow at right edge with margin
                int arrowX = width - DIRECTORY_ARROW_SIZE - DIRECTORY_ARROW_GAP;
                int halfHeight = DIRECTORY_ARROW_SIZE / 2;

                // Triangle points - pointing right
                int[] xPoints = {arrowX, arrowX + DIRECTORY_ARROW_SIZE, arrowX};
                int[] yPoints = {centerY - halfHeight, centerY, centerY + halfHeight};

                g2.setColor(java.awt.Color.DARK_GRAY);
                g2.fillPolygon(xPoints, yPoints, 3);

                g2.setColor(java.awt.Color.WHITE);
                g2.drawPolygon(xPoints, yPoints, 3);

                g2.dispose();
            }
        }
    }
}
