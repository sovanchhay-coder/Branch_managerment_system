package com.bms.ui.panels;

import com.bms.model.Province;
import com.bms.model.User;
import com.bms.service.ProvinceService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ProvincePanel extends BasePanel {

    private final ProvinceService provinceService = new ProvinceService();
    private final JTable table = createTable(List.of("ID", "Province Name"));
    private final JTextField txtName = new JTextField(20);
    private int selectedId = -1;

    public ProvincePanel(User currentUser) {
        super(currentUser, "Provinces");
        initUI();
        loadData();
    }

    private void initUI() {
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Province"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addFormRow(form, gbc, 0, "Name:", txtName);
        JButton btnSave = new JButton("Save");
        JButton btnNew = new JButton("New");
        JButton btnDelete = new JButton("Delete");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btns.add(btnSave);
        btns.add(btnNew);
        btns.add(btnDelete);
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        form.add(btns, gbc);
        add(form, BorderLayout.SOUTH);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                int r = table.convertRowIndexToModel(table.getSelectedRow());
                selectedId = (int) getModel(table).getValueAt(r, 0);
                txtName.setText((String) getModel(table).getValueAt(r, 1));
            }
        });

        btnSave.addActionListener(e -> save());
        btnNew.addActionListener(e -> clearForm());
        btnDelete.addActionListener(e -> delete());
    }

    private void loadData() {
        runBackground(provinceService::findAll, list -> {
            var m = getModel(table);
            m.setRowCount(0);
            for (Province p : list) {
                m.addRow(new Object[]{p.getProvinceId(), p.getProvinceName()});
            }
            clearStatus();
        }, ex -> setStatus("Error: " + ex.getMessage(), true));
    }

    private void save() {
        String name = txtName.getText().trim();
        if (name.isEmpty()) {
            showError(this, "Name is required.");
            return;
        }
        Province p = new Province();
        p.setProvinceName(name);
        if (selectedId <= 0) {
            runBackground(() -> provinceService.create(currentUser, p),
                    created -> {
                        showInfo(this, "Province created.");
                        clearForm();
                        loadData();
                    }, ex -> setStatus("Error: " + ex.getMessage(), true));
        } else {
            p.setProvinceId(selectedId);
            runBackground(() -> {
                provinceService.update(currentUser, p);
                return p;
            }, updated -> {
                showInfo(this, "Province updated.");
                clearForm();
                loadData();
            }, ex -> setStatus("Error: " + ex.getMessage(), true));
        }
    }

    private void delete() {
        if (selectedId <= 0) return;
        if (!confirm(this, "Delete province?")) return;
        runBackground(() -> {
            provinceService.delete(currentUser, selectedId);
            return selectedId;
        }, id -> {
            showInfo(this, "Province deleted.");
            clearForm();
            loadData();
        }, ex -> setStatus("Error: " + ex.getMessage(), true));
    }

    private void clearForm() {
        selectedId = -1;
        txtName.setText("");
        table.clearSelection();
    }
}
