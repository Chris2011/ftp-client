package io.github.chris2011.netbeans.plugins.ftp.client;

import java.awt.Image;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.filechooser.FileSystemView;

import org.openide.util.ImageUtilities;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;

/**
 * Central place for loading icons used by the FTP client module.
 */
public final class FtpIcons {

    public static final String NOTIFICATION_ICON_PATH
        = "io/github/chris2011/netbeans/plugins/ftp/client/ftp_notification.png";
    private static final String CONNECTION_CONNECTED_ICON_PATH
        = "io/github/chris2011/netbeans/plugins/ftp/client/connected.svg";
    private static final String CONNECTION_DISCONNECTED_ICON_PATH
        = "io/github/chris2011/netbeans/plugins/ftp/client/disconnected.svg";

    private static final ImageIcon NOTIFICATION_ICON
        = ImageUtilities.loadImageIcon(NOTIFICATION_ICON_PATH, false);
    private static final Image CONNECTION_CONNECTED_IMAGE
        = ImageUtilities.loadImage(CONNECTION_CONNECTED_ICON_PATH);
    private static final Image CONNECTION_DISCONNECTED_IMAGE
        = ImageUtilities.loadImage(CONNECTION_DISCONNECTED_ICON_PATH);
    private static final Image FOLDER_IMAGE
        = ImageUtilities.loadImage("org/openide/loaders/defaultFolder.gif", false);
    private static final Image FILE_IMAGE
        = ImageUtilities.loadImage("org/openide/loaders/defaultNode.gif", false);

    private static final Icon CONNECTION_CONNECTED_ICON = safeImage2Icon(CONNECTION_CONNECTED_IMAGE);
    private static final Icon CONNECTION_DISCONNECTED_ICON = safeImage2Icon(CONNECTION_DISCONNECTED_IMAGE);
    private static final Icon FOLDER_ICON = safeImage2Icon(FOLDER_IMAGE);
    private static final Icon FILE_ICON = safeImage2Icon(FILE_IMAGE);

    private FtpIcons() {
    }

    /**
     * Safely converts an Image to an Icon, handling null images gracefully.
     * @param image the image to convert, may be null
     * @return an Icon, or null if the image is null
     */
    private static Icon safeImage2Icon(Image image) {
        return image != null ? ImageUtilities.image2Icon(image) : null;
    }

    /**
     * Gets an appropriate icon for a file based on its extension using NetBeans DataObject system.
     * @param filename the filename to get icon for
     * @return an Icon for the file type, matching Project/Files tab appearance
     */
    public static Icon getFileIconByExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return getFileIcon();
        }

        try {
            // Create a temporary file object to get the proper NetBeans icon
            String extension = getFileExtension(filename);
            if (!extension.isEmpty()) {
                // Try to get DataObject for this file type to get the proper icon
                String tempFileName = "temp." + extension;
                try {
                    DataObject dob = DataObject.find(FileUtil.createMemoryFileSystem().getRoot().createData(tempFileName));
                    Image icon = dob.getNodeDelegate().getIcon(java.beans.BeanInfo.ICON_COLOR_16x16);
                    if (icon != null) {
                        return ImageUtilities.image2Icon(icon);
                    }
                } catch (Exception e) {
                    // Fallback to hardcoded icons
                }
            }

            // Fallback to specific known file types
            String ext = extension.toLowerCase();
            Image icon = null;

            switch (ext) {
                case "java":
                    icon = ImageUtilities.loadImage("org/netbeans/modules/java/resources/class.gif", false);
                    break;
                case "js":
                    icon = ImageUtilities.loadImage("org/netbeans/modules/javascript2/editor/resources/javascript_16.png", false);
                    break;
                case "html":
                case "htm":
                    icon = ImageUtilities.loadImage("org/netbeans/modules/html/resources/html.gif", false);
                    break;
                case "css":
                    icon = ImageUtilities.loadImage("org/netbeans/modules/css/visual/resources/style_sheet_16.png", false);
                    break;
                case "xml":
                    icon = ImageUtilities.loadImage("org/netbeans/modules/xml/resources/xmlObject.gif", false);
                    break;
                case "json":
                    icon = ImageUtilities.loadImage("org/netbeans/modules/javascript2/editor/resources/json_16.png", false);
                    break;
                case "txt":
                    icon = ImageUtilities.loadImage("org/openide/loaders/text.gif", false);
                    break;
                case "png":
                case "jpg":
                case "jpeg":
                case "gif":
                case "bmp":
                    icon = ImageUtilities.loadImage("org/openide/loaders/image.gif", false);
                    break;
                default:
                    icon = ImageUtilities.loadImage("org/openide/loaders/unknown.gif", false);
                    break;
            }

            return icon != null ? ImageUtilities.image2Icon(icon) : getFileIcon();

        } catch (Exception e) {
            return getFileIcon();
        }
    }

    private static String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1);
        }
        return "";
    }

    public static Icon getNotificationIcon() {
        return NOTIFICATION_ICON;
    }

    public static Icon getFolderIcon() {
        return FOLDER_ICON;
    }

    public static Image getFolderImage() {
        return FOLDER_IMAGE;
    }

    public static Icon getFileIcon() {
        return FILE_ICON;
    }

    public static Image getFileImage() {
        return FILE_IMAGE;
    }

    public static Icon getConnectionIcon(boolean connected) {
        return connected ? CONNECTION_CONNECTED_ICON : CONNECTION_DISCONNECTED_ICON;
    }

    public static Image getConnectionImage(boolean connected) {
        return connected ? CONNECTION_CONNECTED_IMAGE : CONNECTION_DISCONNECTED_IMAGE;
    }
}
