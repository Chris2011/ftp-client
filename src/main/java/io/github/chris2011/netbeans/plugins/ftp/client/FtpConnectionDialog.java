package io.github.chris2011.netbeans.plugins.ftp.client;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;

public class FtpConnectionDialog extends JDialog {
    private FtpConnection connection;
    private boolean result = false;
    
    private JTextField nameField;
    private JTextField hostField;
    private JSpinner portSpinner;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JCheckBox passiveModeCheckBox;
    
    public FtpConnectionDialog() {
        this(null);
    }
    
    public FtpConnectionDialog(FtpConnection connection) {
        super((JFrame) WindowManager.getDefault().getMainWindow(), true);
        this.connection = connection;
        
        setTitle(connection == null ? "Add FTP Connection" : "Edit FTP Connection");
        initComponents();
        if (connection != null) {
            populateFields();
        }
        pack();
        setLocationRelativeTo(getParent());
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(3, 3, 3, 3);
        
        gbc.gridx = 0; gbc.gridy = 0;
        contentPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        nameField = new JTextField(20);
        contentPanel.add(nameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        contentPanel.add(new JLabel("Host:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        hostField = new JTextField(20);
        contentPanel.add(hostField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        contentPanel.add(new JLabel("Port:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        portSpinner = new JSpinner(new SpinnerNumberModel(21, 1, 65535, 1));
        contentPanel.add(portSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        contentPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        usernameField = new JTextField(20);
        contentPanel.add(usernameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        contentPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        passwordField = new JPasswordField(20);
        contentPanel.add(passwordField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        passiveModeCheckBox = new JCheckBox("Use passive mode");
        passiveModeCheckBox.setSelected(true);
        contentPanel.add(passiveModeCheckBox, gbc);
        
        add(contentPanel, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        
        okButton.addActionListener(new ActionListener() {
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
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
        
        getRootPane().setDefaultButton(okButton);
    }
    
    private void populateFields() {
        nameField.setText(connection.getName());
        hostField.setText(connection.getHost());
        portSpinner.setValue(connection.getPort());
        usernameField.setText(connection.getUsername());
        passwordField.setText(connection.getPassword());
        passiveModeCheckBox.setSelected(connection.isPassiveMode());
    }
    
    private boolean validateInput() {
        if (nameField.getText().trim().isEmpty()) {
            return false;
        }
        if (hostField.getText().trim().isEmpty()) {
            return false;
        }
        if (usernameField.getText().trim().isEmpty()) {
            return false;
        }
        return true;
    }
    
    public boolean showDialog() {
        setVisible(true);
        return result;
    }
    
    public FtpConnection getConnection() {
        if (!result) return null;
        
        FtpConnection conn = new FtpConnection();
        conn.setName(nameField.getText().trim());
        conn.setHost(hostField.getText().trim());
        conn.setPort((Integer) portSpinner.getValue());
        conn.setUsername(usernameField.getText().trim());
        conn.setPassword(new String(passwordField.getPassword()));
        conn.setPassiveMode(passiveModeCheckBox.isSelected());
        
        return conn;
    }
}