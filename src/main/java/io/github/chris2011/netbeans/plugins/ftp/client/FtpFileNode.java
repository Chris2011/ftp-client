package io.github.chris2011.netbeans.plugins.ftp.client;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.time.format.DateTimeFormatter;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node.Property;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;

class FtpFileNode extends AbstractNode {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final FtpExplorerTopComponent explorerComponent;
    private final FtpFile file;
    private final Action openAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            FtpFileOpener.openFile(file, explorerComponent.getFtpClient());
        }
    };

    FtpFileNode(FtpFile file, FtpExplorerTopComponent explorerComponent) {
        super(childrenFor(file, explorerComponent));
        this.file = file;
        this.explorerComponent = explorerComponent;
        setName(file.getName());
        setDisplayName(file.getName());
        setShortDescription(buildTooltip(file));
    }

    private static Children childrenFor(FtpFile file, FtpExplorerTopComponent explorerComponent) {
        if (file.isDirectory()) {
            return Children.create(new FtpFileChildren(file, explorerComponent), true);
        }
        return Children.LEAF;
    }

    @Override
    public Image getIcon(int type) {
        Image icon;
        if (file.isRoot()) {
            icon = FtpIcons.getConnectionImage(explorerComponent.getConnection().isConnected());
        } else if (file.isDirectory()) {
            icon = FtpIcons.getFolderImage();
        } else {
            // Use file extension specific icon for files
            Icon fileIcon = FtpIcons.getFileIconByExtension(file.getName());
            if (fileIcon != null) {
                // Convert Icon to Image properly
                if (fileIcon instanceof ImageIcon) {
                    icon = ((ImageIcon) fileIcon).getImage();
                } else {
                    // For other Icon types, convert using ImageUtilities
                    icon = org.openide.util.ImageUtilities.icon2Image(fileIcon);
                }
            } else {
                icon = FtpIcons.getFileImage();
            }
        }
        return icon != null ? icon : super.getIcon(type);
    }

    @Override
    public Image getOpenedIcon(int type) {
        return getIcon(type);
    }

    @Override
    public Action getPreferredAction() {
        if (file.isFile()) {
            return openAction;
        }
        return super.getPreferredAction();
    }

    // Direct getter methods for OutlineView - these are called directly by column names
    public String getSize() {
        return file.isDirectory() ? "" : file.getFormattedSize();
    }

    public String getModified() {
        return file.isRoot() ? "" : DATE_FORMATTER.format(file.getLastModified());
    }

    public String getPermissions() {
        return file.isRoot() ? "" : file.getPermissionsWithOctal();
    }

    public String getOwner() {
        return file.isRoot() ? "" : file.getOwnerDisplay();
    }

    @Override
    protected Sheet createSheet() {
        Sheet sheet = Sheet.createDefault();
        Sheet.Set set = Sheet.createPropertiesSet();

        // Keep properties for property sheet view, but OutlineView uses direct getters
        Property<String> sizeProperty = new PropertySupport.ReadOnly<String>("size", String.class, "Size", "File size") {
            @Override
            public String getValue() {
                return getSize();
            }
        };
        set.put(sizeProperty);

        Property<String> modifiedProperty = new PropertySupport.ReadOnly<String>("modified", String.class, "Modified", "Last modified") {
            @Override
            public String getValue() {
                return getModified();
            }
        };
        set.put(modifiedProperty);

        Property<String> permissionsProperty = new PropertySupport.ReadOnly<String>("permissions", String.class, "Permissions", "File permissions") {
            @Override
            public String getValue() {
                return getPermissions();
            }
        };
        set.put(permissionsProperty);

        Property<String> ownerProperty = new PropertySupport.ReadOnly<String>("owner", String.class, "Owner", "Owner and group") {
            @Override
            public String getValue() {
                return getOwner();
            }
        };
        set.put(ownerProperty);

        sheet.put(set);
        return sheet;
    }

    private String buildTooltip(FtpFile file) {
        StringBuilder tooltip = new StringBuilder("<html><b>").append(file.getName()).append("</b><br>");
        tooltip.append("Path: ").append(file.getPath()).append("<br>");
        if (file.isRoot()) {
            tooltip.append("Connection: ").append(explorerComponent.getConnection().getDisplayName());
            tooltip.append("</html>");
            return tooltip.toString();
        }

        tooltip.append("Type: ").append(file.isDirectory() ? "Directory" : "File");
        if (!file.isDirectory()) {
            tooltip.append("<br>Size: ").append(file.getFormattedSize());
        }
        tooltip.append("<br>Modified: ").append(file.getLastModified().format(DATE_FORMATTER));
        if (!file.getOwnerDisplay().isEmpty()) {
            tooltip.append("<br>Owner: ").append(file.getOwnerDisplay());
        }
        tooltip.append("<br>Permissions: ").append(file.getPermissions());
        tooltip.append("</html>");
        return tooltip.toString();
    }
}
