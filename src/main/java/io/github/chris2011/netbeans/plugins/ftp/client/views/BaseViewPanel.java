package io.github.chris2011.netbeans.plugins.ftp.client.views;

import io.github.chris2011.netbeans.plugins.ftp.client.FtpExplorerTopComponent;
import io.github.chris2011.netbeans.plugins.ftp.client.FtpFile;
import java.io.IOException;
import java.util.List;
import javax.swing.JPanel;

public abstract class BaseViewPanel extends JPanel {

    protected final FtpExplorerTopComponent parentComponent;

    public BaseViewPanel(FtpExplorerTopComponent parent) {
        this.parentComponent = parent;
    }

    public abstract void refresh();

    public abstract void clear();

    protected List<FtpFile> listFiles(String path) throws IOException {
        return parentComponent.listFiles(path);
    }

    protected boolean isConnected() {
        return parentComponent.isConnected();
    }
}