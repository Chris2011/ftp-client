package io.github.chris2011.netbeans.plugins.ftp.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;

public class FtpConnectionDialog extends JDialog {

    private FtpConnection connection;
    private boolean result = false;
    private boolean passwordOnlyMode = false;

    // Modern UI fields like Cyberduck
    private JComboBox<String> protocolComboBox;
    private JTextField nameField;
    private JTextField serverField;
    private JSpinner portSpinner;
    private JTextField urlField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JCheckBox anonymousLoginCheckBox;
    private JCheckBox savePasswordCheckBox;
    private JCheckBox passiveModeCheckBox;

    // Advanced options panel
    private JPanel advancedPanel;
    private JCheckBox advancedToggle;
    private boolean advancedExpanded = false;

    public FtpConnectionDialog() {
        this(null);
    }

    public FtpConnectionDialog(FtpConnection connection) {
        this(connection, false);
    }

    public FtpConnectionDialog(FtpConnection connection, boolean passwordOnlyMode) {
        super((JFrame) WindowManager.getDefault().getMainWindow(), true);
        this.connection = connection;
        this.passwordOnlyMode = passwordOnlyMode;

        if (passwordOnlyMode) {
            setTitle("Enter Password for " + connection.getName());
        } else {
            setTitle(connection == null ? "Neue Verbindung" : "Verbindung bearbeiten");
        }

        initComponents();
        if (connection != null && !passwordOnlyMode) {
            populateFields();
        }
        pack();
        setLocationRelativeTo(getParent());
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(450, passwordOnlyMode ? 150 : 380));

        if (passwordOnlyMode) {
            initPasswordOnlyMode();
        } else {
            initFullMode();
        }

        createButtonPanel();
    }

    private void initPasswordOnlyMode() {
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBorder(new EmptyBorder(16, 16, 16, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(8, 0, 8, 0);

        // Connection info
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel connectionLabel = new JLabel("Verbindung: " + connection.getDisplayName());
        connectionLabel.setFont(connectionLabel.getFont().deriveFont(Font.BOLD));
        contentPanel.add(connectionLabel, gbc);

        // Password field
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        contentPanel.add(new JLabel("Passwort:"), gbc);

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        passwordField = new JPasswordField(20);
        passwordField.setPreferredSize(new Dimension(250, 25));
        contentPanel.add(passwordField, gbc);

        add(contentPanel, BorderLayout.CENTER);
    }

    private void initFullMode() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(16, 16, 0, 16));

        // Protocol and name section
        JPanel protocolPanel = createProtocolPanel();
        mainPanel.add(protocolPanel);
        mainPanel.add(Box.createVerticalStrut(12));

        // Server and port section
        JPanel serverPanel = createServerPanel();
        mainPanel.add(serverPanel);
        mainPanel.add(Box.createVerticalStrut(8));

        // URL section
        JPanel urlPanel = createUrlPanel();
        mainPanel.add(urlPanel);
        mainPanel.add(Box.createVerticalStrut(12));

        // Credentials section
        JPanel credentialsPanel = createCredentialsPanel();
        mainPanel.add(credentialsPanel);
        mainPanel.add(Box.createVerticalStrut(12));

        // Advanced options
        createAdvancedPanel();
        mainPanel.add(advancedToggle);
        mainPanel.add(advancedPanel);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createProtocolPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Protocol dropdown with icon
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        protocolComboBox = new JComboBox<>(new String[]{
            "🗂 FTP (Unverschlüsselte Verbindung)",
            "🔐 FTP-SSL (Explicit AUTH TLS)",
            "🔒 SFTP (SSH Verbindung)",
            "☁ WebDAV (HTTP)",
            "🔒 WebDAV (HTTPS)",
            "💾 SMB (Server Message Block)",
            "☁ Amazon S3",
            "☁ Google Cloud Storage",
            "📁 Dropbox",
            "🌐 Google Drive",
            "💼 Microsoft OneDrive",
            "💼 Microsoft SharePoint",
            "📦 Box"
        });
        protocolComboBox.setPreferredSize(new Dimension(350, 28));
        protocolComboBox.addActionListener(e -> onProtocolChanged());
        panel.add(protocolComboBox, gbc);

        return panel;
    }

    private JPanel createServerPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Server label and field
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 4, 0);
        panel.add(new JLabel("Server:"), gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.7;
        gbc.insets = new Insets(0, 0, 0, 8);
        serverField = new JTextField();
        serverField.setPreferredSize(new Dimension(240, 25));
        panel.add(serverField, gbc);

        // Port label and field
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE; gbc.insets = new Insets(0, 0, 4, 0);
        panel.add(new JLabel("Port:"), gbc);

        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.3;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.insets = new Insets(0, 0, 0, 0);
        portSpinner = new JSpinner(new SpinnerNumberModel(21, 1, 65535, 1));
        portSpinner.setPreferredSize(new Dimension(80, 25));
        panel.add(portSpinner, gbc);

        return panel;
    }

    private JPanel createUrlPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 4, 0);
        panel.add(new JLabel("URL:"), gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0);
        urlField = new JTextField();
        urlField.setPreferredSize(new Dimension(350, 25));
        urlField.setForeground(Color.BLUE);
        urlField.setEditable(false);
        panel.add(urlField, gbc);

        return panel;
    }

    private JPanel createCredentialsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Username
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 4, 0);
        panel.add(new JLabel("Benutzername:"), gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 8, 0);
        usernameField = new JTextField();
        usernameField.setPreferredSize(new Dimension(350, 25));
        panel.add(usernameField, gbc);

        // Password
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        gbc.insets = new Insets(0, 0, 4, 0);
        panel.add(new JLabel("Passwort:"), gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 8, 0);
        passwordField = new JPasswordField();
        passwordField.setPreferredSize(new Dimension(350, 25));
        panel.add(passwordField, gbc);

        // Checkboxes
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        gbc.insets = new Insets(0, 0, 4, 0);
        anonymousLoginCheckBox = new JCheckBox("Anonymer Login");
        panel.add(anonymousLoginCheckBox, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 0, 0);
        savePasswordCheckBox = new JCheckBox("Passwort speichern");
        savePasswordCheckBox.setSelected(true);
        panel.add(savePasswordCheckBox, gbc);

        // Anonymous login listener
        anonymousLoginCheckBox.addItemListener(e -> {
            boolean anonymous = e.getStateChange() == ItemEvent.SELECTED;
            usernameField.setEnabled(!anonymous);
            passwordField.setEnabled(!anonymous);
            savePasswordCheckBox.setEnabled(!anonymous);
            if (anonymous) {
                usernameField.setText("anonymous");
                passwordField.setText("");
            } else {
                usernameField.setText("");
            }
        });

        return panel;
    }

    private void createAdvancedPanel() {
        // Toggle button
        advancedToggle = new JCheckBox("▶ Erweiterte Optionen");
        advancedToggle.addActionListener(e -> toggleAdvancedOptions());

        // Advanced panel (initially hidden)
        advancedPanel = new JPanel(new GridBagLayout());
        advancedPanel.setBorder(BorderFactory.createTitledBorder("Erweiterte Optionen"));
        advancedPanel.setVisible(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        passiveModeCheckBox = new JCheckBox("Passiver Modus verwenden");
        passiveModeCheckBox.setSelected(true);
        advancedPanel.add(passiveModeCheckBox, gbc);

        // Name field in advanced
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.insets = new Insets(8, 0, 4, 0);
        advancedPanel.add(new JLabel("Verbindungsname:"), gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0);
        nameField = new JTextField();
        nameField.setPreferredSize(new Dimension(350, 25));
        advancedPanel.add(nameField, gbc);
    }

    private void toggleAdvancedOptions() {
        advancedExpanded = !advancedExpanded;
        advancedPanel.setVisible(advancedExpanded);
        advancedToggle.setText(advancedExpanded ? "▼ Erweiterte Optionen" : "▶ Erweiterte Optionen");
        pack();
    }

    private void createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(new EmptyBorder(8, 16, 16, 16));

        JButton connectButton = new JButton(passwordOnlyMode ? "Verbinden" : "Verbinden");
        JButton cancelButton = new JButton("Abbrechen");

        // Style buttons like Cyberduck
        connectButton.setPreferredSize(new Dimension(100, 30));
        cancelButton.setPreferredSize(new Dimension(100, 30));

        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (validateInput()) {
                    result = true;
                    dispose();
                }
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                result = false;
                dispose();
            }
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(connectButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Set connect button as default button for Enter key
        getRootPane().setDefaultButton(connectButton);

        // Add listeners for dynamic URL updating
        if (!passwordOnlyMode) {
            addUrlUpdateListeners();
        }
    }

    private void addUrlUpdateListeners() {
        ActionListener updateUrl = e -> updateUrlField();

        serverField.addActionListener(updateUrl);
        serverField.addCaretListener(e -> updateUrlField());
        portSpinner.addChangeListener(e -> updateUrlField());
        usernameField.addCaretListener(e -> updateUrlField());
    }

    private void onProtocolChanged() {
        String selectedProtocol = (String) protocolComboBox.getSelectedItem();
        if (selectedProtocol == null) return;

        // Set default port based on protocol
        int defaultPort = getDefaultPortForProtocol(selectedProtocol);
        portSpinner.setValue(defaultPort);

        // Update URL field
        updateUrlField();
    }

    private int getDefaultPortForProtocol(String protocol) {
        if (protocol.contains("FTP-SSL")) return 21;
        if (protocol.contains("SFTP")) return 22;
        if (protocol.contains("WebDAV (HTTPS)")) return 443;
        if (protocol.contains("WebDAV (HTTP)")) return 80;
        if (protocol.contains("SMB")) return 445;
        if (protocol.contains("Amazon S3")) return 443;
        if (protocol.contains("Google Cloud")) return 443;
        if (protocol.contains("Dropbox")) return 443;
        if (protocol.contains("Google Drive")) return 443;
        if (protocol.contains("OneDrive")) return 443;
        if (protocol.contains("SharePoint")) return 443;
        if (protocol.contains("Box")) return 443;
        return 21; // FTP default
    }

    private String getProtocolPrefixForUrl(String protocol) {
        if (protocol.contains("FTP-SSL")) return "ftps";
        if (protocol.contains("SFTP")) return "sftp";
        if (protocol.contains("WebDAV (HTTPS)")) return "https";
        if (protocol.contains("WebDAV (HTTP)")) return "http";
        if (protocol.contains("SMB")) return "smb";
        if (protocol.contains("Amazon S3")) return "s3";
        if (protocol.contains("Google Cloud")) return "gs";
        if (protocol.contains("Dropbox")) return "dropbox";
        if (protocol.contains("Google Drive")) return "gdrive";
        if (protocol.contains("OneDrive")) return "onedrive";
        if (protocol.contains("SharePoint")) return "sharepoint";
        if (protocol.contains("Box")) return "box";
        return "ftp"; // FTP default
    }

    private void updateUrlField() {
        String server = serverField.getText().trim();
        int port = (Integer) portSpinner.getValue();
        String username = usernameField.getText().trim();
        String selectedProtocol = (String) protocolComboBox.getSelectedItem();

        if (!server.isEmpty() && selectedProtocol != null) {
            String protocolPrefix = getProtocolPrefixForUrl(selectedProtocol);
            int defaultPort = getDefaultPortForProtocol(selectedProtocol);

            StringBuilder url = new StringBuilder(protocolPrefix).append("://");
            if (!username.isEmpty()) {
                url.append(username).append("@");
            }
            url.append(server);
            if (port != defaultPort) {
                url.append(":").append(port);
            }
            url.append("/");
            urlField.setText(url.toString());
        } else {
            urlField.setText("");
        }
    }

    private void populateFields() {
        if (nameField != null) nameField.setText(connection.getName());
        if (serverField != null) serverField.setText(connection.getHost());
        if (portSpinner != null) portSpinner.setValue(connection.getPort());
        if (usernameField != null) usernameField.setText(connection.getUsername());
        if (passwordField != null) passwordField.setText(connection.getPassword());
        if (passiveModeCheckBox != null) passiveModeCheckBox.setSelected(connection.isPassiveMode());
        if (savePasswordCheckBox != null) savePasswordCheckBox.setSelected(true);

        // Update URL field if available
        if (urlField != null) {
            updateUrlField();
        }
    }

    private boolean validateInput() {
        if (passwordOnlyMode) {
            // Only validate password field
            return passwordField.getPassword().length > 0;
        } else {
            // Validate required fields
            if (serverField.getText().trim().isEmpty()) {
                return false;
            }
            if (!anonymousLoginCheckBox.isSelected() && usernameField.getText().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean showDialog() {
        setVisible(true);
        return result;
    }

    public FtpConnection getConnection() {
        if (!result) {
            return null;
        }

        FtpConnection conn = connection != null ? new FtpConnection(connection) : new FtpConnection();

        // Set name (auto-generate if empty)
        String name = (nameField != null && !nameField.getText().trim().isEmpty())
            ? nameField.getText().trim()
            : serverField.getText().trim();
        conn.setName(name);

        conn.setHost(serverField.getText().trim());
        conn.setPort((Integer) portSpinner.getValue());
        conn.setUsername(usernameField.getText().trim());
        conn.setPassword(new String(passwordField.getPassword()));
        conn.setPassiveMode(passiveModeCheckBox != null ? passiveModeCheckBox.isSelected() : true);

        return conn;
    }

    public String getEnteredPassword() {
        return new String(passwordField.getPassword());
    }
}
