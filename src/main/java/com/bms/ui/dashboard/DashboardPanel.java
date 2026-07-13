package com.bms.ui.dashboard;

import com.bms.model.User;
import com.bms.service.UserService;
import com.bms.ui.AppFrame;
import com.bms.ui.panels.*;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class DashboardPanel extends JPanel {

    public static final String PANEL_INVENTORY = "Inventory";
    public static final String PANEL_PRODUCTS = "Products";
    public static final String PANEL_CATEGORIES = "Categories";
    public static final String PANEL_WAREHOUSES = "Warehouses";
    public static final String PANEL_PROVINCES = "Provinces";
    public static final String PANEL_USERS = "Users";
    public static final String PANEL_TRANSFERS = "Transfers";
    public static final String PANEL_REPORTS = "Reports";
    public static final String PANEL_POS = "POS";

    private final AppFrame appFrame;
    private final User currentUser;
    private final List<String> permissions;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private final UserService userService = new UserService();

    public DashboardPanel(AppFrame appFrame, User currentUser, List<String> permissions) {
        this.appFrame = appFrame;
        this.currentUser = currentUser;
        this.permissions = permissions;
        setLayout(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        JLabel lblUser = new JLabel("Logged in: " + currentUser.getUsername() + " (" + currentUser.getRoleName() + ")");
        lblUser.setFont(lblUser.getFont().deriveFont(Font.BOLD));
        header.add(lblUser, BorderLayout.WEST);
        JButton btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> appFrame.logout());
        header.add(btnLogout, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildSidebar(), contentPanel);
        split.setDividerLocation(220);
        add(split, BorderLayout.CENTER);

        registerPanels();
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel lblMenu = new JLabel("Menu");
        lblMenu.setFont(lblMenu.getFont().deriveFont(Font.BOLD, 16f));
        lblMenu.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(lblMenu);
        sidebar.add(Box.createVerticalStrut(12));

        boolean canManageMain = permissions.contains("manage_main_warehouse");
        boolean canManageProvince = permissions.contains("manage_province_warehouse");
        boolean canControlStore = permissions.contains("control_store_warehouse");
        boolean canSell = permissions.contains("sell_product");
        boolean canReport = permissions.contains("view_reports");

        boolean canCreateUsers = false;
        try {
            canCreateUsers = !userService.getCreatableRoles(currentUser).isEmpty();
        } catch (SQLException ignored) {}

        if (canManageMain || canManageProvince || canControlStore) {
            addNavButton(sidebar, "Inventory", PANEL_INVENTORY);
        }
        if (canManageMain) {
            addNavButton(sidebar, "Products", PANEL_PRODUCTS);
            addNavButton(sidebar, "Categories", PANEL_CATEGORIES);
            addNavButton(sidebar, "Warehouses", PANEL_WAREHOUSES);
            addNavButton(sidebar, "Provinces", PANEL_PROVINCES);
        }
        if (canCreateUsers) {
            addNavButton(sidebar, "Users", PANEL_USERS);
        }
        if (canManageMain || canManageProvince) {
            addNavButton(sidebar, "Transfers", PANEL_TRANSFERS);
        }
        if (canReport) {
            addNavButton(sidebar, "Reports", PANEL_REPORTS);
        }
        if (canSell) {
            addNavButton(sidebar, "POS", PANEL_POS);
        }

        sidebar.add(Box.createVerticalGlue());
        return sidebar;
    }

    private void addNavButton(JPanel sidebar, String label, String panelName) {
        JButton btn = new JButton(label);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, btn.getPreferredSize().height));
        btn.addActionListener(e -> cardLayout.show(contentPanel, panelName));
        sidebar.add(btn);
        sidebar.add(Box.createVerticalStrut(6));
    }

    private void registerPanels() {
        contentPanel.add(new InventoryPanel(currentUser), PANEL_INVENTORY);
        contentPanel.add(new ProductPanel(currentUser), PANEL_PRODUCTS);
        contentPanel.add(new ProvinceCategoryPanel(currentUser), PANEL_CATEGORIES);
        contentPanel.add(new WarehousePanel(currentUser), PANEL_WAREHOUSES);
        contentPanel.add(new ProvincePanel(currentUser), PANEL_PROVINCES);
        contentPanel.add(new UserPanel(currentUser), PANEL_USERS);
        contentPanel.add(new TransferPanel(currentUser), PANEL_TRANSFERS);
        contentPanel.add(new ReportPanel(currentUser), PANEL_REPORTS);
        contentPanel.add(new POSPanel(currentUser), PANEL_POS);
    }

    public void showPanel(String name) {
        cardLayout.show(contentPanel, name);
    }
}
