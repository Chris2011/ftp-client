package io.github.chris2011.netbeans.plugins.ftp.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.SwingUtilities;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.NotificationDisplayer;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.RequestProcessor;

public class FtpFileOpener {

    private static final RequestProcessor RP = new RequestProcessor("FtpFileOpener");

    public static void openFile(FtpFile ftpFile, FtpClient ftpClient) {
        RP.post(() -> {
            try {
                // Download file to temp directory
                Path tempDir = Files.createTempDirectory("ftp-client-");
                Path tempFile = tempDir.resolve(ftpFile.getName());

                // Download file content
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                org.apache.commons.net.ftp.FTPClient client = ftpClient.ftpClient;

                if (client.retrieveFile(ftpFile.getPath(), baos)) {
                    // Write to temp file
                    try (FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {
                        fos.write(baos.toByteArray());
                    }

                    // Open in NetBeans editor
                    SwingUtilities.invokeLater(() -> {
                        try {
                            FileObject fileObj = FileUtil.toFileObject(tempFile.toFile());
                            if (fileObj != null) {
                                DataObject dataObj = DataObject.find(fileObj);
                                OpenCookie openCookie = dataObj.getLookup().lookup(OpenCookie.class);
                                if (openCookie != null) {
                                    openCookie.open();

                                    // Set up auto-save listener
                                    setupAutoSave(fileObj, ftpFile, ftpClient, tempFile);

                                    // Show success notification
                                    org.openide.awt.NotificationDisplayer.getDefault().notify(
                                        "FTP File Opened",
                                        FtpIcons.getNotificationIcon(),
                                        "Opened " + ftpFile.getName() + " from FTP server",
                                        null,
                                        NotificationDisplayer.Priority.LOW
                                    );
                                }
                            }
                        } catch (Exception ex) {
                            String errorMsg = "Failed to open file in editor: " + ftpFile.getName();
                            String details = ex.getMessage();
                            showErrorDialog(errorMsg + "\n\nDetails: " + details);

                            // Also show notification
                            org.openide.awt.NotificationDisplayer.getDefault().notify(
                                "FTP File Open Failed",
                                FtpIcons.getNotificationIcon(),
                                errorMsg,
                                null,
                                org.openide.awt.NotificationDisplayer.Priority.HIGH
                            );
                        }
                    });
                } else {
                    String errorMsg = "Failed to download file from FTP server: " + ftpFile.getName();
                    String details = "FTP retrieve operation failed. Check file existence and permissions.";
                    SwingUtilities.invokeLater(() -> {
                        showErrorDialog(errorMsg + "\n\nDetails: " + details);
                        org.openide.awt.NotificationDisplayer.getDefault().notify(
                            "FTP Download Failed",
                            FtpIcons.getNotificationIcon(),
                            errorMsg,
                            null,
                            org.openide.awt.NotificationDisplayer.Priority.HIGH
                        );
                    });
                }
            } catch (IOException ex) {
                String errorMsg = "Error downloading file: " + ftpFile.getName();
                String details = "I/O Error: " + ex.getMessage();
                SwingUtilities.invokeLater(() -> {
                    showErrorDialog(errorMsg + "\n\nDetails: " + details);
                    org.openide.awt.NotificationDisplayer.getDefault().notify(
                        "FTP Download Error",
                        FtpIcons.getNotificationIcon(),
                        errorMsg,
                        null,
                        org.openide.awt.NotificationDisplayer.Priority.HIGH
                    );
                });
            }
        });
    }

    private static void setupAutoSave(FileObject fileObj, FtpFile ftpFile, FtpClient ftpClient, Path tempFile) {
        // Add file change listener for auto-save
        fileObj.addFileChangeListener(new org.openide.filesystems.FileChangeAdapter() {
            @Override
            public void fileChanged(org.openide.filesystems.FileEvent fe) {
                // Auto-save to FTP when file changes
                RP.post(() -> {
                    try {
                        if (Files.exists(tempFile)) {
                            byte[] content = Files.readAllBytes(tempFile);
                            ByteArrayInputStream bais = new ByteArrayInputStream(content);

                            org.apache.commons.net.ftp.FTPClient client = ftpClient.ftpClient;
                            if (client.isConnected() && client.storeFile(ftpFile.getPath(), bais)) {
                                // Show success notification
                                SwingUtilities.invokeLater(() -> {
                                    org.openide.awt.NotificationDisplayer.getDefault().notify(
                                        "FTP File Saved",
                                        FtpIcons.getNotificationIcon(),
                                        "Auto-saved " + ftpFile.getName() + " to FTP server",
                                        null
                                    );
                                });
                            } else {
                                String errorMsg = "Failed to auto-save " + ftpFile.getName() + " to FTP server";
                                String details = client.isConnected() ?
                                    "FTP store operation failed. Check file permissions and disk space." :
                                    "FTP connection lost. Please reconnect and try again.";

                                SwingUtilities.invokeLater(() -> {
                                    showErrorDialog(errorMsg + "\n\nDetails: " + details);
                                    // Also show notification for better visibility
                                    org.openide.awt.NotificationDisplayer.getDefault().notify(
                                        "FTP Save Failed",
                                        FtpIcons.getNotificationIcon(),
                                        details,
                                        null,
                                        org.openide.awt.NotificationDisplayer.Priority.HIGH
                                    );
                                });
                            }
                        }
                    } catch (IOException ex) {
                        String errorMsg = "Auto-save error for " + ftpFile.getName();
                        String details = "I/O Error: " + ex.getMessage();

                        SwingUtilities.invokeLater(() -> {
                            showErrorDialog(errorMsg + "\n\nDetails: " + details);
                            // Also show notification for better visibility
                            org.openide.awt.NotificationDisplayer.getDefault().notify(
                                "FTP Save Error",
                                FtpIcons.getNotificationIcon(),
                                details,
                                null,
                                org.openide.awt.NotificationDisplayer.Priority.HIGH
                            );
                        });
                    }
                });
            }
        });
    }

    private static void showErrorDialog(String message) {
        SwingUtilities.invokeLater(() -> {
            NotifyDescriptor descriptor = new NotifyDescriptor.Message(
                message,
                NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(descriptor);
        });
    }
}
