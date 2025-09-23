package io.github.chris2011.netbeans.plugins.ftp.client;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import org.apache.commons.net.ftp.FTPFile;

public class FtpFile {

    private final String name;
    private final String path;
    private final boolean isDirectory;
    private final long size;
    private final LocalDateTime lastModified;
    private final String permissions;

    private final String owner;
    private final String group;
    private final boolean root;

    public FtpFile(String path, FTPFile ftpFile) {
        this(
            ftpFile.getName(),
            path.endsWith("/") ? path + ftpFile.getName() : path + "/" + ftpFile.getName(),
            ftpFile.isDirectory(),
            ftpFile.getSize(),
            toLocalDateTime(ftpFile.getTimestamp() != null ? ftpFile.getTimestamp().getTime() : new Date()),
            extractPermissions(ftpFile),
            nullToEmpty(ftpFile.getUser()),
            nullToEmpty(ftpFile.getGroup()),
            false
        );
    }

    public FtpFile(String name, String path, boolean isDirectory) {
        this(name, path, isDirectory, 0, LocalDateTime.now(),
            isDirectory ? "drwxr-xr-x" : "-rw-r--r--", "", "", false);
    }

    private FtpFile(String name, String path, boolean isDirectory, long size,
        LocalDateTime lastModified, String permissions, String owner, String group, boolean root) {
        this.name = name;
        this.path = path;
        this.isDirectory = isDirectory;
        this.size = size;
        this.lastModified = lastModified;
        this.permissions = permissions;
        this.owner = owner;
        this.group = group;
        this.root = root;
    }

    public static FtpFile createRoot(String displayName) {
        return new FtpFile(displayName, "/", true, 0, LocalDateTime.now(),
            "drwxr-xr-x", "", "", true);
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public boolean isFile() {
        return !isDirectory;
    }

    public long getSize() {
        return size;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public String getPermissions() {
        return permissions;
    }

    public String getPermissionsWithOctal() {
        if (permissions.length() >= 10) {
            String octal = convertPermissionsToOctal(permissions);
            return permissions + " (" + octal + ")";
        }
        return permissions;
    }

    private String convertPermissionsToOctal(String perms) {
        if (perms.length() < 10) return "";

        // Skip first character (file type: d, -, l, etc.)
        String userPerms = perms.substring(1, 4);   // rwx
        String groupPerms = perms.substring(4, 7);  // rwx
        String otherPerms = perms.substring(7, 10); // rwx

        int userOctal = permStringToOctal(userPerms);
        int groupOctal = permStringToOctal(groupPerms);
        int otherOctal = permStringToOctal(otherPerms);

        return String.format("%d%d%d", userOctal, groupOctal, otherOctal);
    }

    private int permStringToOctal(String perm) {
        int value = 0;
        if (perm.charAt(0) == 'r') value += 4; // read
        if (perm.charAt(1) == 'w') value += 2; // write
        if (perm.charAt(2) == 'x') value += 1; // execute
        return value;
    }

    public String getOwner() {
        return owner;
    }

    public String getGroup() {
        return group;
    }

    public boolean isRoot() {
        return root;
    }

    public String getOwnerDisplay() {
        if (owner.isEmpty() && group.isEmpty()) {
            return "";
        }
        if (group.isEmpty()) {
            return owner;
        }
        if (owner.isEmpty()) {
            return group;
        }
        return owner + ":" + group;
    }

    public String getParentPath() {
        if (path.equals("/")) {
            return null;
        }
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash <= 0) {
            return "/";
        }
        return path.substring(0, lastSlash);
    }

    public String getFormattedSize() {
        if (isDirectory) {
            return "";
        }

        if (size < 1024) {
            return size + " B";
        }
        if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        }
        if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        }
        return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        FtpFile ftpFile = (FtpFile) obj;
        return path.equals(ftpFile.path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    private static LocalDateTime toLocalDateTime(Date timestamp) {
        return timestamp.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private static String extractPermissions(FTPFile ftpFile) {
        String formatted = ftpFile.toFormattedString();
        if (formatted != null && formatted.length() >= 10) {
            return formatted.substring(0, 10);
        }
        return ftpFile.isDirectory() ? "drwxr-xr-x" : "-rw-r--r--";
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
