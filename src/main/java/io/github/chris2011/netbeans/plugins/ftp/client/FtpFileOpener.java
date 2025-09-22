package io.github.chris2011.netbeans.plugins.ftp.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.SwingUtilities;
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
                                        null,
                                        "Opened " + ftpFile.getName() + " from FTP server",
                                        null
                                    );
                                }
                            }
                        } catch (Exception ex) {
                            showErrorNotification("Failed to open file in editor: " + ex.getMessage());
                        }
                    });
                } else {
                    showErrorNotification("Failed to download file from FTP server");
                }
            } catch (IOException ex) {
                showErrorNotification("Error downloading file: " + ex.getMessage());
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
                                        null,
                                        "Auto-saved " + ftpFile.getName() + " to FTP server",
                                        null
                                    );
                                });
                            } else {
                                SwingUtilities.invokeLater(() -> {
                                    showErrorNotification("Failed to auto-save " + ftpFile.getName() + " to FTP server");
                                });
                            }
                        }
                    } catch (IOException ex) {
                        SwingUtilities.invokeLater(() -> {
                            showErrorNotification("Auto-save error: " + ex.getMessage());
                        });
                    }
                });
            }
        });
    }
    
    private static void showErrorNotification(String message) {
        SwingUtilities.invokeLater(() -> {
            org.openide.awt.NotificationDisplayer.getDefault().notify(
                "FTP File Error",
                null,
                message,
                null
            );
        });
    }
}