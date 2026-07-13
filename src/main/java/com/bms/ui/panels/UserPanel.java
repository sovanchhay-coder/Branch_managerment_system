package com.bms.ui.panels;

import com.bms.model.*;
import com.bms.service.BranchService;
import com.bms.service.UserService;
import com.bms.service.WarehouseService;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserPanel extends BasePanel {

    private final UserService userService = new UserService();
    private final BranchService branchService = new BranchService();
    private final WarehouseService warehouseService = new WarehouseService();

    private final JTable table = createTable(List.of("ID", "Username", "Role", "Branch", "Warehouse"));
    private final JTextField txtUsername = new JTextField(20);
    private final JPasswordField txtPassword = new JPasswordField(20);
    private final JComboBox<Role> comboRole = new JComboBox<>();
    private final JComboBox<Branch> comboBranch = new JComboBox<>();
    private final JComboBox<Warehouse> comboWarehouse = new JComboBox<>();

    public UserPanel(User currentUser) {
        super(currentUser, "User Management");
        initUI();
        loadData();
    }

    private void initUI() {
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Create User"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addFormRow(form, gbc, 0, "Username:", txtUsername);
        addFormRow(form, gbc, 1, "Password:", txtPassword);
        addFormRow(form, gbc, 2, "Role:", comboRole);
        addFormRow(form, gbc, 3, "Branch:", comboBranch);
        addFormRow(form, gbc, 4, "Warehouse:", comboWarehouse);

        JButton btnCreate = new JButton("Create User");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btns.add(btnCreate);
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        form.add(btns, gbc);
        add(form, BorderLayout.SOUTH);

        comboRole.addActionListener(e -> updateScopeDefaults());

        btnCreate.addActionListener(e -> createUser());
    }

    private void loadData() {
        runBackground(() -> {
            List<Role> roles = userService.getCreatableRoles(currentUser);
            List<Branch> branches = getScopedBranches();
            List<Warehouse> warehouses = getScopedWarehouses();
            List<User> users = getScopedUsers();
            return new Data(roles, branches, warehouses, users);
        }, data -> {
            comboRole.removeAllItems();
            for (Role r : data.roles) comboRole.addItem(r);

            comboBranch.removeAllItems();
            comboBranch.addItem(new Branch() {{ setBranchId(-1); setBranchName("(none)"); }});
            for (Branch b : data.branches) comboBranch.addItem(b);

            comboWarehouse.removeAllItems();
            Warehouse none = new Warehouse();
            none.setWarehouseId(-1);
            none.setName("(none)");
            comboWarehouse.addItem(none);
            for (Warehouse w : data.warehouses) comboWarehouse.addItem(w);

            var m = getModel(table);
            m.setRowCount(0);
            for (User u : data.users) {
                m.addRow(new Object[]{u.getUserId(), u.getUsername(), u.getRoleName(),
                        u.getBranchName(), u.getWarehouseName()});
            }
            updateScopeDefaults();
            clearStatus();
        }, ex -> setStatus("Error: " + ex.getMessage(), true));
    }

    private List<Branch> getScopedBranches() throws SQLException {
        int level = currentUser.getRoleLevel();
        if (level == 0) return branchService.findAll();
        if (level == 1) {
            Integer province = resolveCreatorProvince();
            if (province != null) return branchService.findByProvince(province);
        }
        if (level == 2 && currentUser.getBranchId() != null) {
            return branchService.findById(currentUser.getBranchId()).map(List::of).orElse(List.of());
        }
        return List.of();
    }

    private List<Warehouse> getScopedWarehouses() throws SQLException {
        int level = currentUser.getRoleLevel();
        if (level == 0) return warehouseService.findAll();
        if (level == 1) {
            Integer province = resolveCreatorProvince();
            if (province != null) return warehouseService.findByProvince(province);
        }
        return List.of();
    }

    private List<User> getScopedUsers() throws SQLException {
        List<User> all = userService.findAll();
        int level = currentUser.getRoleLevel();
        if (level == 0) return all;
        if (level == 1) {
            Integer province = resolveCreatorProvince();
            if (province == null) return List.of();
            List<User> filtered = new ArrayList<>();
            for (User u : all) {
                Integer up = resolveUserProvince(u);
                if (province.equals(up)) filtered.add(u);
            }
            return filtered;
        }
        if (level == 2) {
            return all.stream()
                    .filter(u -> currentUser.getBranchId() != null && currentUser.getBranchId().equals(u.getBranchId()))
                    .toList();
        }
        return List.of();
    }

    private Integer resolveCreatorProvince() throws SQLException {
        if (currentUser.getBranchId() != null) {
            return branchService.findById(currentUser.getBranchId()).map(Branch::getProvinceId).orElse(null);
        }
        if (currentUser.getWarehouseId() != null) {
            return warehouseService.findById(currentUser.getWarehouseId()).map(Warehouse::getProvinceId).orElse(null);
        }
        return null;
    }

    private Integer resolveUserProvince(User u) throws SQLException {
        if (u.getBranchId() != null) {
            return branchService.findById(u.getBranchId()).map(Branch::getProvinceId).orElse(null);
        }
        if (u.getWarehouseId() != null) {
            return warehouseService.findById(u.getWarehouseId()).map(Warehouse::getProvinceId).orElse(null);
        }
        return null;
    }

    private void updateScopeDefaults() {
        Role role = (Role) comboRole.getSelectedItem();
        if (role == null) return;
        int level = currentUser.getRoleLevel();
        if (level == 2) {
            // Manager creating Sale: lock branch to own branch.
            for (int i = 0; i < comboBranch.getItemCount(); i++) {
                if (comboBranch.getItemAt(i).getBranchId() == currentUser.getBranchId()) {
                    comboBranch.setSelectedIndex(i);
                    break;
                }
            }
            comboBranch.setEnabled(false);
        } else {
            comboBranch.setEnabled(true);
        }
    }

    private void createUser() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());
        Role role = (Role) comboRole.getSelectedItem();
        Branch branch = (Branch) comboBranch.getSelectedItem();
        Warehouse warehouse = (Warehouse) comboWarehouse.getSelectedItem();
        if (username.isEmpty() || password.isEmpty() || role == null) {
            showError(this, "Username, password and role are required.");
            return;
        }

        Integer branchId = (branch != null && branch.getBranchId() > 0) ? branch.getBranchId() : null;
        Integer warehouseId = (warehouse != null && warehouse.getWarehouseId() > 0) ? warehouse.getWarehouseId() : null;

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setRoleId(role.getRoleId());
        newUser.setBranchId(branchId);
        newUser.setWarehouseId(warehouseId);

        runBackground(() -> userService.createUser(currentUser, newUser, password),
                created -> {
                    showInfo(this, "User created: " + created.getUsername());
                    txtUsername.setText("");
                    txtPassword.setText("");
                    loadData();
                }, ex -> setStatus("Error: " + ex.getMessage(), true));
    }

    private record Data(List<Role> roles, List<Branch> branches, List<Warehouse> warehouses, List<User> users) {}
}
