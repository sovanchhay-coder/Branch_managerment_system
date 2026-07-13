package com.bms.ui.panels;

import com.bms.model.*;
import com.bms.service.InventoryService;
import com.bms.service.ProductService;
import com.bms.service.WarehouseService;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InventoryPanel extends BasePanel {

    private final InventoryService inventoryService = new InventoryService();
    private final ProductService productService = new ProductService();
    private final WarehouseService warehouseService = new WarehouseService();

    private final JTable table = createTable(List.of("ID", "Product", "Warehouse", "Qty", "Last Updated"));
    private final JComboBox<Warehouse> filterWarehouse = new JComboBox<>();
    private final JComboBox<Product> comboProduct = new JComboBox<>();
    private final JComboBox<Warehouse> comboWarehouse = new JComboBox<>();
    private final JTextField txtQty = new JTextField(8);

    public InventoryPanel(User currentUser) {
        super(currentUser, "Inventory");
        initUI();
        loadData();
    }

    private void initUI() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Filter warehouse:"));
        top.add(filterWarehouse);
        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(e -> loadData());
        top.add(btnRefresh);
        filterWarehouse.addActionListener(e -> loadData());
        add(top, BorderLayout.NORTH);

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Stock Adjustment"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addFormRow(form, gbc, 0, "Product:", comboProduct);
        addFormRow(form, gbc, 1, "Warehouse:", comboWarehouse);
        addFormRow(form, gbc, 2, "Quantity:", txtQty);

        JButton btnAdd = new JButton("Add / Remove");
        JButton btnSet = new JButton("Set Quantity");
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnPanel.add(btnAdd);
        btnPanel.add(btnSet);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        form.add(btnPanel, gbc);
        add(form, BorderLayout.SOUTH);

        btnAdd.addActionListener(e -> adjust(true));
        btnSet.addActionListener(e -> adjust(false));
    }

    private void loadData() {
        runBackground(() -> {
            List<Warehouse> warehouses = getScopedWarehouses();
            List<Product> products = productService.findAll();
            List<Inventory> inventory;
            Warehouse selected = (Warehouse) filterWarehouse.getSelectedItem();
            if (selected != null && selected.getWarehouseId() > 0) {
                inventory = inventoryService.findByWarehouse(selected.getWarehouseId());
            } else {
                inventory = inventoryService.findAll();
            }
            return new Data(warehouses, products, inventory);
        }, data -> {
            filterWarehouse.removeActionListener(filterWarehouse.getActionListeners()[0]);
            filterWarehouse.removeAllItems();
            filterWarehouse.addItem(new Warehouse() {{ setWarehouseId(-1); setName("All"); }});
            for (Warehouse w : data.warehouses) filterWarehouse.addItem(w);
            filterWarehouse.addActionListener(e -> loadData());

            comboWarehouse.removeAllItems();
            for (Warehouse w : data.warehouses) comboWarehouse.addItem(w);

            comboProduct.removeAllItems();
            for (Product p : data.products) comboProduct.addItem(p);

            var m = getModel(table);
            m.setRowCount(0);
            for (Inventory i : data.inventory) {
                m.addRow(new Object[]{i.getInventoryId(), i.getProductName(), i.getWarehouseName(), i.getQuantity(), i.getLastUpdated()});
            }
            clearStatus();
        }, ex -> setStatus("Error: " + ex.getMessage(), true));
    }

    private List<Warehouse> getScopedWarehouses() throws SQLException {
        List<Warehouse> list = new ArrayList<>();
        int level = currentUser.getRoleLevel();
        if (level == 0) {
            list.addAll(warehouseService.findAll());
        } else if (level == 1) {
            Integer province = resolveCreatorProvince();
            if (province != null) {
                list.addAll(warehouseService.findByProvince(province));
            }
        } else if (level >= 2) {
            // Manager / Sale: only their store warehouse.
            Branch branch = null;
            if (currentUser.getBranchId() != null) {
                try {
                    branch = new com.bms.db.BranchDAO().findById(currentUser.getBranchId()).orElse(null);
                } catch (SQLException ignored) {}
            }
            if (branch != null && branch.getWarehouseId() != null) {
                Warehouse w = warehouseService.findById(branch.getWarehouseId()).orElse(null);
                if (w != null) list.add(w);
            }
        }
        return list;
    }

    private Integer resolveCreatorProvince() throws SQLException {
        if (currentUser.getBranchId() != null) {
            return new com.bms.db.BranchDAO().findById(currentUser.getBranchId()).map(Branch::getProvinceId).orElse(null);
        }
        if (currentUser.getWarehouseId() != null) {
            return new com.bms.db.WarehouseDAO().findById(currentUser.getWarehouseId()).map(Warehouse::getProvinceId).orElse(null);
        }
        return null;
    }

    private void adjust(boolean addMode) {
        Product p = (Product) comboProduct.getSelectedItem();
        Warehouse w = (Warehouse) comboWarehouse.getSelectedItem();
        if (p == null || w == null) {
            showError(this, "Select product and warehouse.");
            return;
        }
        int qty;
        try {
            qty = Integer.parseInt(txtQty.getText().trim());
        } catch (NumberFormatException ex) {
            showError(this, "Quantity must be a number.");
            return;
        }

        final int finalQty = qty;
        if (addMode) {
            runBackground(() -> inventoryService.adjust(currentUser, p.getProductId(), w.getWarehouseId(), finalQty, "manual adjustment"),
                    result -> {
                        showInfo(this, "Stock updated. New quantity: " + result);
                        loadData();
                    }, ex -> setStatus("Error: " + ex.getMessage(), true));
        } else {
            runBackground(() -> {
                inventoryService.setQuantity(currentUser, p.getProductId(), w.getWarehouseId(), finalQty);
                return finalQty;
            }, result -> {
                showInfo(this, "Stock set to " + result);
                loadData();
            }, ex -> setStatus("Error: " + ex.getMessage(), true));
        }
    }

    private record Data(List<Warehouse> warehouses, List<Product> products, List<Inventory> inventory) {}
}
