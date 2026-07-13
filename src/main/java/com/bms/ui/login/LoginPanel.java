package com.bms.ui.login;

import com.bms.model.User;
import com.bms.service.AuthService;
import com.bms.ui.AppFrame;
import com.bms.db.PermissionDAO;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class LoginPanel extends JPanel {

    private final AppFrame appFrame;
    private final AuthService authService = new AuthService();
    private final PermissionDAO permissionDAO = new PermissionDAO();
    private final JTextField txtUsername = new JTextField(20);
    private final JPasswordField txtPassword = new JPasswordField(20);
    private final JButton btnLogin = new JButton("Login");
    private final JLabel lblStatus = new JLabel(" ");

    public LoginPanel(AppFrame appFrame) {
        this.appFrame = appFrame;
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblTitle = new JLabel("Branch Management System", SwingConstants.CENTER);
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 22f));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        add(lblTitle, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1; gbc.gridx = 0;
        add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        add(txtUsername, gbc);

        gbc.gridy = 2; gbc.gridx = 0;
        add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        add(txtPassword, gbc);

        gbc.gridy = 3; gbc.gridx = 0; gbc.gridwidth = 2;
        btnLogin.setFont(btnLogin.getFont().deriveFont(Font.BOLD));
        add(btnLogin, gbc);

        gbc.gridy = 4;
        lblStatus.setForeground(Color.RED);
        lblStatus.setHorizontalAlignment(SwingConstants.CENTER);
        add(lblStatus, gbc);

        btnLogin.addActionListener(e -> attemptLogin());
        txtPassword.addActionListener(e -> attemptLogin());
    }

    private void attemptLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            lblStatus.setText("Enter username and password.");
            return;
        }

        btnLogin.setEnabled(false);
        lblStatus.setText("Authenticating...");

        new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() throws Exception {
                return authService.authenticate(username, password)
                        .orElseThrow(() -> new SecurityException("Invalid username or password."));
            }

            @Override
            protected void done() {
                try {
                    User user = get();
                    List<String> perms = permissionDAO.findNamesByRoleId(user.getRoleId());
                    txtPassword.setText("");
                    lblStatus.setText(" ");
                    appFrame.onLoginSuccess(user, perms);
                } catch (Exception ex) {
                    lblStatus.setText(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                } finally {
                    btnLogin.setEnabled(true);
                }
            }
        }.execute();
    }
}
