package io.github.chris2011.netbeans.plugins.ftp.client;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
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
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.openide.util.ImageUtilities;

public class MillerColumnsPanel extends JPanel {

    private final FtpExplorerTopComponent parentComponent;
    private final List<JList<FtpFile>> columns;
    private final JPanel columnsContainer;
    private final JScrollPane scrollPane;
    private String currentPath = "/";
    private int currentColumnIndex = 0;

    public MillerColumnsPanel(FtpExplorerTopComponent parent) {
        this.parentComponent = parent;
        this.columns = new ArrayList<>();

        setLayout(new BorderLayout());

        columnsContainer = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                // Calculate exact size needed for all columns
                int totalWidth = columns.size() * 220; // 220px per column
                int height = getParent() != null ? getParent().getHeight() : 400;
                Dimension size = new Dimension(totalWidth, height);
                System.out.println("Container preferred size: " + size + " for " + columns.size() + " columns");
                return size;
            }

            @Override
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }

            @Override
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }
        };
        columnsContainer.setLayout(new BoxLayout(columnsContainer, BoxLayout.X_AXIS));

        scrollPane = new JScrollPane(columnsContainer);

        // Add mouse wheel listener to container for horizontal scrolling with modifiers
        columnsContainer.addMouseWheelListener(e -> {
            if ((e.isShiftDown() || e.isControlDown())) {
                // Horizontal scrolling with Shift+wheel or Ctrl+wheel
                int scrollAmount = e.getUnitsToScroll() * 30;
                int currentValue = scrollPane.getHorizontalScrollBar().getValue();
                int newValue = Math.max(0, Math.min(
                    scrollPane.getHorizontalScrollBar().getMaximum() - scrollPane.getHorizontalScrollBar().getVisibleAmount(),
                    currentValue + scrollAmount));
                scrollPane.getHorizontalScrollBar().setValue(newValue);
                e.consume();
            }
        });
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        // Improve horizontal scrolling for smooth experience
        scrollPane.getHorizontalScrollBar().setUnitIncrement(32);
        scrollPane.getHorizontalScrollBar().setBlockIncrement(220); // One column width

        // Enable horizontal mouse wheel scrolling with modifier keys on the scroll pane itself
        scrollPane.addMouseWheelListener(e -> {
            if ((e.isShiftDown() || e.isControlDown())) {
                // Horizontal scroll with Shift+Wheel or Ctrl+Wheel
                int scrollAmount = e.getUnitsToScroll() * 30;
                int currentValue = scrollPane.getHorizontalScrollBar().getValue();
                int newValue = Math.max(0, Math.min(
                    scrollPane.getHorizontalScrollBar().getMaximum() - scrollPane.getHorizontalScrollBar().getVisibleAmount(),
                    currentValue + scrollAmount));
                scrollPane.getHorizontalScrollBar().setValue(newValue);
                e.consume();
            }
        });

        add(scrollPane, BorderLayout.CENTER);

        // Make panel focusable and add keyboard navigation
        setFocusable(true);
        addKeyListener(new MillerKeyListener());

        // Add mouse wheel listener to the main panel as fallback
        this.addMouseWheelListener(e -> {
            if ((e.isShiftDown() || e.isControlDown())) {
                // Horizontal scroll with Shift+Wheel or Ctrl+Wheel
                int scrollAmount = e.getUnitsToScroll() * 30;
                int currentValue = scrollPane.getHorizontalScrollBar().getValue();
                int newValue = Math.max(0, Math.min(
                    scrollPane.getHorizontalScrollBar().getMaximum() - scrollPane.getHorizontalScrollBar().getVisibleAmount(),
                    currentValue + scrollAmount));
                scrollPane.getHorizontalScrollBar().setValue(newValue);
                e.consume();
            }
        });
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
        currentColumnIndex = 0;
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

        // No parent directory (..) entry in miller columns
        // The previous column already represents the parent level

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

        JList<FtpFile> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new FtpFileListCellRenderer());

        // Add keyboard navigation to each list
        list.addKeyListener(new MillerKeyListener());

        // Remove automatic directory expansion on selection
        // Directories will only open with explicit actions (double-click or right arrow key)

        // Add double-click listener for file/directory opening
        list.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    FtpFile selectedFile = list.getSelectedValue();
                    if (selectedFile != null) {
                        if (selectedFile.isDirectory()) {
                            // Open directory in new column
                            onDirectorySelected(selectedFile, list);
                        } else {
                            // Open file in editor
                            openFileInEditor(selectedFile);
                        }
                    }
                }
            }
        });

        JScrollPane columnScrollPane = new JScrollPane(list);
        // Fixed width - no resizing, no shrinking
        columnScrollPane.setPreferredSize(new Dimension(220, 0));
        columnScrollPane.setMinimumSize(new Dimension(220, 0)); // Fixed minimum = preferred
        columnScrollPane.setMaximumSize(new Dimension(220, Integer.MAX_VALUE)); // Fixed maximum = preferred
        columnScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        columnsContainer.add(columnScrollPane);
        columns.add(list);

        // Don't add horizontal scroll listeners to individual columns - let them handle vertical scrolling normally

        // Update current column index to the newly added column
        currentColumnIndex = columns.size() - 1;

        // Set focus on the newly added column
        SwingUtilities.invokeLater(() -> {
            list.requestFocusInWindow();
            if (list.getModel().getSize() > 0 && list.getSelectedIndex() == -1) {
                list.setSelectedIndex(0);
            }
        });

        // Force container size recalculation
        columnsContainer.revalidate();
        columnsContainer.repaint();

        revalidate();
        repaint();

        // Auto-scroll to show the last column completely
        SwingUtilities.invokeLater(() -> {
            scrollToLastColumn();
        });
    }

    private void scrollToLastColumn() {
        if (!columns.isEmpty()) {
            // Wait for layout to complete with double-invokeLater for better timing
            SwingUtilities.invokeLater(() -> SwingUtilities.invokeLater(() -> {
                // Calculate the position to scroll to show the last column completely
                final int COLUMN_WIDTH = 220; // Fixed width
                int totalWidth = columns.size() * COLUMN_WIDTH;
                int viewportWidth = scrollPane.getViewport().getWidth();

                System.out.println("Scroll calculation: " + columns.size() + " columns, total width: " + totalWidth + ", viewport: " + viewportWidth);

                if (totalWidth > viewportWidth && viewportWidth > 0) {
                    // Scroll to show the last column completely
                    int scrollPosition = Math.max(0, totalWidth - viewportWidth);

                    // Ensure we don't scroll beyond the maximum
                    int maxScroll = scrollPane.getHorizontalScrollBar().getMaximum() - scrollPane.getHorizontalScrollBar().getVisibleAmount();
                    scrollPosition = Math.min(scrollPosition, maxScroll);

                    scrollPane.getHorizontalScrollBar().setValue(scrollPosition);
                    System.out.println("Auto-scrolled to position: " + scrollPosition + " (max: " + maxScroll + ")");
                } else {
                    // Reset scroll position if all columns fit
                    scrollPane.getHorizontalScrollBar().setValue(0);
                    System.out.println("All columns fit, reset scroll to 0");
                }

                // Force complete update
                scrollPane.revalidate();
                scrollPane.repaint();
            }));
        }
    }

    private void onDirectorySelected(FtpFile directory, JList<FtpFile> sourceList) {
        int columnIndex = columns.indexOf(sourceList);

        // Remove all columns to the right of the selected one
        for (int i = columns.size() - 1; i > columnIndex; i--) {
            columnsContainer.remove(i);
            columns.remove(i);
        }

        String newPath = directory.getPath();
        currentPath = newPath;
        loadPath(newPath);
    }

    private String getParentPath(String path) {
        if (path.equals("/")) {
            return "/";
        }
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash <= 0) {
            return "/";
        }
        return path.substring(0, lastSlash);
    }

    private void openFileInEditor(FtpFile file) {
        FtpFileOpener.openFile(file, parentComponent.getFtpClient());
    }



    private static class FtpFileListCellRenderer extends DefaultListCellRenderer {

        private static final Icon FOLDER_ICON = FtpIcons.getFolderIcon();
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

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

                // Create display text with ellipsis support
                String fileName = file.getName();

                if (file.isFile()) {
                    // For files: show name + size/date in HTML (fixed width)
                    String displayText = "<html><div style='width: 170px; overflow: hidden; white-space: nowrap; text-overflow: ellipsis;'>"
                        + "<b>" + fileName + "</b><br>"
                        + "<small>" + file.getFormattedSize() + " - " + file.getLastModified().format(DATE_FORMATTER) + "</small>"
                        + "</div></html>";
                    setText(displayText);
                    setToolTipText(fileName); // Show full name in tooltip
                } else {
                    // For directories: use HTML with dynamic width based on available space
                    // Check if scrollbar might be visible by looking at list size
                    int textWidth = 180; // Default width
                    if (list != null && list.getModel().getSize() > 10) { // Heuristic for scrollbar
                        textWidth = 160; // Reduced width when scrollbar likely visible
                    }
                    String displayText = "<html><div style='width: " + textWidth + "px; overflow: hidden; white-space: nowrap; text-overflow: ellipsis;'>"
                        + fileName + "</div></html>";
                    setText(displayText);
                    setToolTipText(fileName); // Show full name in tooltip
                }

                // Reserve space for arrow on directories
                if (isDirectory) {
                    // Add sufficient right margin to prevent text overlap with arrow
                    setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 25));
                } else {
                    setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 2));
                }
            }

            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // Draw triangle indicator for directories at the right edge
            if (isDirectory) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth();
                int height = getHeight();
                int triangleSize = 6;
                int margin = 20; // Fixed margin from right edge

                // Simple arrow positioning - always from right edge with fixed margin
                int arrowX = width - margin - triangleSize;

                // Ensure arrow is visible
                if (arrowX < 0) {
                    arrowX = 2;
                }

                // Triangle points - pointing right
                int[] xPoints = {
                    arrowX,
                    arrowX + triangleSize,
                    arrowX
                };
                int[] yPoints = {
                    height / 2 - triangleSize / 2,
                    height / 2,
                    height / 2 + triangleSize / 2
                };

                // Use a visible color
                g2.setColor(getForeground());
                g2.fillPolygon(xPoints, yPoints, 3);

                // Add border for visibility
                g2.setColor(getForeground().darker());
                g2.drawPolygon(xPoints, yPoints, 3);

                g2.dispose();
            }
        }
    }

    private class MillerKeyListener implements KeyListener {
        @Override
        public void keyPressed(KeyEvent e) {
            if (columns.isEmpty()) {
                return;
            }

            switch (e.getKeyCode()) {
                case KeyEvent.VK_RIGHT:
                    handleRightArrow();
                    break;
                case KeyEvent.VK_LEFT:
                    handleLeftArrow();
                    break;
                case KeyEvent.VK_ENTER:
                    handleEnterKey();
                    break;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            // Not needed
        }

        @Override
        public void keyTyped(KeyEvent e) {
            // Not needed
        }

        private void handleRightArrow() {
            if (currentColumnIndex < columns.size() - 1) {
                // Move focus to next column
                currentColumnIndex++;
                focusColumn(currentColumnIndex);
            } else {
                // Try to expand current selection if it's a directory
                JList<FtpFile> currentList = columns.get(currentColumnIndex);
                FtpFile selectedFile = currentList.getSelectedValue();
                if (selectedFile != null && selectedFile.isDirectory()) {
                    onDirectorySelected(selectedFile, currentList);
                }
            }
        }

        private void handleEnterKey() {
            if (currentColumnIndex >= 0 && currentColumnIndex < columns.size()) {
                JList<FtpFile> currentList = columns.get(currentColumnIndex);
                FtpFile selectedFile = currentList.getSelectedValue();
                if (selectedFile != null) {
                    if (selectedFile.isDirectory()) {
                        // Open directory in new column
                        onDirectorySelected(selectedFile, currentList);
                    } else {
                        // Open file in editor
                        openFileInEditor(selectedFile);
                    }
                }
            }
        }

        private void handleLeftArrow() {
            if (currentColumnIndex > 0) {
                // Remove current column and move focus to previous column
                int columnToRemove = currentColumnIndex;
                columnsContainer.remove(columnToRemove);
                columns.remove(columnToRemove);
                currentColumnIndex--;

                // Update current path to parent directory
                if (currentColumnIndex < columns.size()) {
                    JList<FtpFile> previousList = columns.get(currentColumnIndex);
                    FtpFile selectedFile = previousList.getSelectedValue();
                    if (selectedFile != null) {
                        currentPath = selectedFile.getPath();
                    }
                }

                focusColumn(currentColumnIndex);
                revalidate();
                repaint();
            }
        }

        private void focusColumn(int columnIndex) {
            if (columnIndex >= 0 && columnIndex < columns.size()) {
                JList<FtpFile> targetList = columns.get(columnIndex);
                targetList.requestFocusInWindow();

                // Select first item if nothing is selected
                if (targetList.getSelectedIndex() == -1 && targetList.getModel().getSize() > 0) {
                    targetList.setSelectedIndex(0);
                }
            }
        }
    }
}
