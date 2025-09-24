package io.github.chris2011.netbeans.plugins.ftp.client.views;

import io.github.chris2011.netbeans.plugins.ftp.client.FtpExplorerTopComponent;
import io.github.chris2011.netbeans.plugins.ftp.client.FtpFile;
import io.github.chris2011.netbeans.plugins.ftp.client.FtpFileOpener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

public class MillerColumnsPanel extends BaseViewPanel implements MillerColumn.ColumnListener {

    private final List<MillerColumn> columns;
    private final JPanel columnsContainer;
    private final JScrollPane scrollPane;
    private final MouseWheelListener horizontalScrollListener;
    private final MillerKeyListener keyListener;
    private int currentColumnIndex = 0;

    public MillerColumnsPanel(FtpExplorerTopComponent parent) {
        super(parent);
        this.columns = new ArrayList<>();
        this.keyListener = new MillerKeyListener();

        setLayout(new BorderLayout());

        horizontalScrollListener = this::handleHorizontalScroll;

        columnsContainer = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                int totalWidth = columns.size() * 220; // 220px per column
                int height = getParent() != null ? getParent().getHeight() : 400;
                return new Dimension(totalWidth, height);
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

        installHorizontalScrollSupport(columnsContainer);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Improve horizontal scrolling for smooth experience
        scrollPane.getHorizontalScrollBar().setUnitIncrement(50);
        scrollPane.getHorizontalScrollBar().setBlockIncrement(220); // One column width

        installHorizontalScrollSupport(scrollPane);

        add(scrollPane, BorderLayout.CENTER);

        // Make panel focusable and add keyboard navigation
        setFocusable(true);
        addKeyListener(keyListener);
        
        addMouseWheelListener(horizontalScrollListener);

//        installHorizontalScrollSupport(this);
    }

    @Override
    public void refresh() {
        if (!isConnected()) {
            return;
        }

        clear();
        loadPath("/");
    }

    @Override
    public void clear() {
        columnsContainer.removeAll();
        columns.clear();
        currentColumnIndex = 0;
        revalidate();
        repaint();
    }

    @Override
    public void onDirectorySelected(FtpFile directory, MillerColumn sourceColumn) {
        int columnIndex = columns.indexOf(sourceColumn);

        // Remove all columns to the right of the selected one
        for (int i = columns.size() - 1; i > columnIndex; i--) {
            columnsContainer.remove(i);
            columns.remove(i);
        }

        String newPath = directory.getPath();
        loadPath(newPath);
    }

    public void openFileInEditor(FtpFile file) {
        FtpFileOpener.openFile(file, parentComponent.getFtpClient());
    }

    private void loadPath(String path) {
        SwingUtilities.invokeLater(() -> {
            try {
                List<FtpFile> files = listFiles(path);
                addColumn(files, path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void addColumn(List<FtpFile> files, String path) {
        MillerColumn column = new MillerColumn(files, path, this, horizontalScrollListener, keyListener);

        columnsContainer.add(column);
        columns.add(column);

        // Update current column index to the newly added column
        currentColumnIndex = columns.size() - 1;

        // Set focus on the newly added column
        SwingUtilities.invokeLater(() -> {
            column.requestFocus();
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

    private void installHorizontalScrollSupport(Component component) {
        component.addMouseWheelListener(horizontalScrollListener);
    }

    private void handleHorizontalScroll(MouseWheelEvent e) {
        // Always check for vertical scrolling first - if a column can scroll vertically, don't interfere
        if (hasActiveVerticalScroll(e.getComponent())) {
            return; // Let vertical scrolling take priority
        }

        if (!isHorizontalScrollGesture(e)) {
            return;
        }

        if (!canScrollHorizontally()) {
            return;
        }

        int scrollAmount;
        if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
            double rotation = e.getPreciseWheelRotation();
            if (rotation == 0) {
                rotation = e.getWheelRotation();
            }
            scrollAmount = (int) Math.round(rotation * 50);
            if (scrollAmount == 0) {
                if (rotation == 0) {
                    return;
                }
                scrollAmount = rotation < 0 ? -50 : 50;
            }
        } else {
            scrollAmount = e.getWheelRotation() * scrollPane.getHorizontalScrollBar().getBlockIncrement();
        }

        int currentValue = scrollPane.getHorizontalScrollBar().getValue();
        int maxScroll = scrollPane.getHorizontalScrollBar().getMaximum() - scrollPane.getHorizontalScrollBar().getVisibleAmount();
        int newValue = Math.max(0, Math.min(maxScroll, currentValue + scrollAmount));

        if (newValue == currentValue) {
            return;
        }

        scrollPane.getHorizontalScrollBar().setValue(newValue);
        e.consume(); // Only consume if we actually handled horizontal scrolling
    }

    private boolean isHorizontalScrollGesture(MouseWheelEvent e) {
        if (!canScrollHorizontally()) {
            return false;
        }

        // Only treat as horizontal if explicitly requested with modifier keys
        if (e.isShiftDown() || e.isControlDown()) {
            return true;
        }

        // For trackpad horizontal swipes only - very specific detection
        if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL
            && e.getUnitsToScroll() == 0
            && e.getScrollAmount() == 0
            && e.getWheelRotation() == 0
            && e.getPreciseWheelRotation() != 0d) {
            return true;
        }

        // Default: treat normal wheel scrolling as vertical first
        return false;
    }

    private boolean canScrollHorizontally() {
        JScrollBar horizontalBar = scrollPane.getHorizontalScrollBar();
        if (horizontalBar == null) {
            return false;
        }

        int minimum = horizontalBar.getMinimum();
        int maximum = horizontalBar.getMaximum();
        int visibleAmount = horizontalBar.getVisibleAmount();

        return maximum - minimum > visibleAmount;
    }

    private boolean hasActiveVerticalScroll(Component source) {
        JScrollPane enclosingScrollPane = findEnclosingScrollPane(source);
        if (enclosingScrollPane == null || enclosingScrollPane == scrollPane) {
            return false;
        }

        JScrollBar verticalBar = enclosingScrollPane.getVerticalScrollBar();
        if (verticalBar != null) {
            int minimum = verticalBar.getMinimum();
            int maximum = verticalBar.getMaximum();
            int visibleAmount = verticalBar.getVisibleAmount();

            if (maximum - minimum > visibleAmount) {
                return true;
            }
        }

        JViewport viewport = enclosingScrollPane.getViewport();
        if (viewport != null) {
            Component view = viewport.getView();
            if (view != null) {
                Dimension extent = viewport.getExtentSize();
                Dimension preferred = view.getPreferredSize();

                if (preferred != null && extent != null && preferred.height > extent.height) {
                    return true;
                }

                if (view.getHeight() > viewport.getHeight()) {
                    return true;
                }
            }
        }

        return false;
    }

    private JScrollPane findEnclosingScrollPane(Component component) {
        if (component instanceof JScrollPane) {
            return (JScrollPane) component;
        }
        return (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, component);
    }

    private void scrollToLastColumn() {
        if (!columns.isEmpty()) {
            SwingUtilities.invokeLater(() -> SwingUtilities.invokeLater(() -> {
                final int COLUMN_WIDTH = 220;
                int totalWidth = columns.size() * COLUMN_WIDTH;
                int viewportWidth = scrollPane.getViewport().getWidth();

                if (totalWidth > viewportWidth && viewportWidth > 0) {
                    int scrollPosition = Math.max(0, totalWidth - viewportWidth);
                    int maxScroll = scrollPane.getHorizontalScrollBar().getMaximum() - scrollPane.getHorizontalScrollBar().getVisibleAmount();
                    scrollPosition = Math.min(scrollPosition, maxScroll);
                    scrollPane.getHorizontalScrollBar().setValue(scrollPosition);
                } else {
                    scrollPane.getHorizontalScrollBar().setValue(0);
                }

                scrollPane.revalidate();
                scrollPane.repaint();
            }));
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
                MillerColumn currentColumn = columns.get(currentColumnIndex);
                JList<FtpFile> currentList = currentColumn.getList();
                FtpFile selectedFile = currentList.getSelectedValue();
                if (selectedFile != null && selectedFile.isDirectory()) {
                    onDirectorySelected(selectedFile, currentColumn);
                }
            }
        }

        private void handleEnterKey() {
            if (currentColumnIndex >= 0 && currentColumnIndex < columns.size()) {
                MillerColumn currentColumn = columns.get(currentColumnIndex);
                JList<FtpFile> currentList = currentColumn.getList();
                FtpFile selectedFile = currentList.getSelectedValue();
                if (selectedFile != null) {
                    if (selectedFile.isDirectory()) {
                        onDirectorySelected(selectedFile, currentColumn);
                    } else {
                        // Open file in editor - would need access to FtpClient
                         FtpFileOpener.openFile(selectedFile, parentComponent.getFtpClient());
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

                focusColumn(currentColumnIndex);
                revalidate();
                repaint();
            }
        }

        private void focusColumn(int columnIndex) {
            if (columnIndex >= 0 && columnIndex < columns.size()) {
                MillerColumn targetColumn = columns.get(columnIndex);
                targetColumn.requestFocus();
            }
        }
    }
}