package com.bms.ui.panels;

import com.bms.model.*;
import com.bms.service.ProductService;
import com.bms.service.TransferService;
import com.bms.service.WarehouseService;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TransferPanel extends BasePanel {

    private final TransferService transferService = new TransferService();
    private final WarehouseService warehouseService = new WarehouseService();
    private final ProductService productService = new ProductService();

    private final JTable table = createTable(List.of("ID", "From", "To", "Product", "Qty", "Date", "Approved By"));
    private final JComboBox<Warehouse> comboFrom = new JComboBox<>();
    private final JComboBox<Warehouse> comboTo = new JComboBox<>();
    private final JComboBox<Product> comboProduct = new JComboBox<>();
    private final JTextField txtQty = new JTextField(8);
    private final JCheckBox chkPendingOnly = new JCheckBox("Pending only");

    public TransferPanel(User currentUser) {
        super(currentUser, "Transfers");
        initUI();
        loadData();
    }

    private void initUI() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(chkPendingOnly);
        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(e -> loadData());
        top.add(btnRefresh);
        JButton btnApprove = new JButton("Approve Selected");
        btnApprove.addActionListener(e -> approveSelected());
        top.add(btnApprove);
        add(top, BorderLayout.NORTH);

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Create Transfer"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addFormRow(form, gbc, 0, "From:", comboFrom);
        addFormRow(form, gbc, 1, "To:", comboTo);
        addFormRow(form, gbc, 2, "Product:", comboProduct);
        addFormRow(form, gbc, 3, "Quantity:", txtQty);

        JButton btnCreate = new JButton("Create Transfer");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btns.add(btnCreate);
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        form.add(btns, gbc);
        add(form, BorderLayout.SOUTH);

        btnCreate.addActionListener(e -> createTransfer());
        chkPendingOnly.addActionListener(e -> loadData());
    }

    private void loadData() {
        runBackground(() -> new Data(
                chkPendingOnly.isSelected() ? transferService.findPending() : transferService.findAll(),
                getScopedWarehouses(),
                productService.findAll()),
                data -> {
                    comboFrom.removeAllItems();
                    comboTo.removeAllItems();
                    for (Warehouse w : data.warehouses) {
                        comboFrom.addItem(w);
                        comboTo.addItem(w);
                    }
                    comboProduct.removeAllItems();
                    for (Product p : data.products) comboProduct.addItem(p);

                    var m = getModel(table);
                    m.setRowCount(0);
                    for (Transfer t : data.transfers) {
                        m.addRow(new Object[]{t.getTransferId(), t.getFromWarehouseName(), t.getToWarehouseName(),
                                t.getProductName(), t.getQuantity(), t.getTransferDate(),
                                t.getApprovedByName() != null ? t.getApprovedByName() : "Pending"});
                    }
                    clearStatus();
                }, ex -> setStatus("Error: " + ex.getMessage(), true));
    }

    private List<Warehouse> getScopedWarehouses() throws SQLException {
        java.util.LinkedHashMap<Integer, Warehouse> map = new java.util.LinkedHashMap<>();
        int level = currentUser.getRoleLevel();
        if (level == 0) {
            for (Warehouse w : warehouseService.findAll()) map.put(w.getWarehouseId(), w);
        } else if (level == 1) {
            Integer province = resolveCreatorProvince();
            if (province != null) {
                for (Warehouse w : warehouseService.findByProvince(province)) map.put(w.getWarehouseId(), w);
            }
            // Sup admin may also transfer from the main warehouse.
            for (Warehouse w : warehouseService.findByType(1)) map.put(w.getWarehouseId(), w);
        }
        return new ArrayList<>(map.values());
    }

    private Integer resolveCreatorProvince() throws SQLException {
        if (currentUser.getBranchId() != null) {
            return new com.bms.db.BranchDAO().findById(currentUser.getBranchId()).map(Branch::getProvinceId).orElse(null);
        }
        if (currentUser.getWarehouseId() != null) {
            return warehouseService.findById(currentUser.getWarehouseId()).map(Warehouse::getProvinceId).orElse(null);
        }
        return null;
    }

    private void createTransfer() {
        Warehouse from = (Warehouse) comboFrom.getSelectedItem();
        Warehouse to = (Warehouse) comboTo.getSelectedItem();
        Product p = (Product) comboProduct.getSelectedItem();
        int qty;
        try {
            qty = Integer.parseInt(txtQty.getText().trim());
        } catch (NumberFormatException ex) {
            showError(this, "Quantity must be a number.");
            return;
        }
        if (from == null || to == null || p == null || qty <= 0) {
            showError(this, "Select valid from/to warehouses, product and quantity.");
            return;
        }
        Transfer t = new Transfer();
        t.setFromWarehouseId(from.getWarehouseId());
        t.setToWarehouseId(to.getWarehouseId());
        t.setProductId(p.getProductId());
        t.setQuantity(qty);

        runBackground(() -> transferService.create(currentUser, t),
                created -> {
                    showInfo(this, "Transfer created (pending approval).");
                    loadData();
                }, ex -> setStatus("Error: " + ex.getMessage(), true));
    }

    private void approveSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            showError(this, "Select a transfer to approve.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        int transferId = (int) getModel(table).getValueAt(modelRow, 0);
        String approved = (String) getModel(table).getValueAt(modelRow, 6);
        if (!"Pending".equals(approved)) {
            showError(this, "Transfer is already approved.");
            return;
        }
        if (!confirm(this, "Approve transfer " + transferId + "?")) return;
        runBackground(() -> {
            transferService.approve(currentUser, transferId);
            return transferId;
        }, id -> {
            showInfo(this, "Transfer approved.");
            loadData();
        }, ex -> setStatus("Error: " + ex.getMessage(), true));
    }

    private record Data(List<Transfer> transfers, List<Warehouse> warehouses, List<Product> products) {}
}
