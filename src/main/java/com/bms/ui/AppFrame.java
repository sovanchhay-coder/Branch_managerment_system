package com.bms.ui;

import com.bms.model.User;
import com.bms.ui.dashboard.DashboardPanel;
import com.bms.ui.login.LoginPanel;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class AppFrame extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainPanel = new JPanel(cardLayout);
    private User currentUser;
    private List<String> permissions;
    private DashboardPanel dashboardPanel;

    public AppFrame() {
        setTitle("Branch Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setLocationRelativeTo(null);

        mainPanel.add(new LoginPanel(this), "LOGIN");
        add(mainPanel);
        showCard("LOGIN");
    }

    public void onLoginSuccess(User user, List<String> perms) {
        this.currentUser = user;
        this.permissions = perms;
        if (dashboardPanel != null) {
            mainPanel.remove(dashboardPanel);
        }
        dashboardPanel = new DashboardPanel(this, user, perms);
        mainPanel.add(dashboardPanel, "DASHBOARD");
        showCard("DASHBOARD");
    }

    public void logout() {
        currentUser = null;
        permissions = null;
        if (dashboardPanel != null) {
            mainPanel.remove(dashboardPanel);
            dashboardPanel = null;
        }
        showCard("LOGIN");
    }

    public void showCard(String name) {
        cardLayout.show(mainPanel, name);
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                FlatLightLaf.setup();
                UIManager.setLookAndFeel(new FlatLightLaf());
            } catch (UnsupportedLookAndFeelException e) {
                System.err.println("FlatLaf not available, using default L&F");
            }
            new AppFrame().setVisible(true);
        });
    }
}
