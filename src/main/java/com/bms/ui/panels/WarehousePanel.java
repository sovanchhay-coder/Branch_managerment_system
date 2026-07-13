package com.bms.ui.panels;

import com.bms.model.Province;
import com.bms.model.User;
import com.bms.model.Warehouse;
import com.bms.model.WarehouseType;
import com.bms.service.ProvinceService;
import com.bms.service.WarehouseService;
import com.bms.service.WarehouseTypeService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class WarehousePanel extends BasePanel {

    private final WarehouseService warehouseService = new WarehouseService();
    private final ProvinceService provinceService = new ProvinceService();
    private final WarehouseTypeService typeService = new WarehouseTypeService();

    private final JTable table = createTable(List.of("ID", "Name", "Type", "Province", "Parent", "Location"));
    private final JTextField txtName = new JTextField(20);
    private final JComboBox<WarehouseType> comboType = new JComboBox<>();
    private final JComboBox<Province> comboProvince = new JComboBox<>();
    private final JComboBox<Warehouse> comboParent = new JComboBox<>();
    private final JTextField txtLocation = new JTextField(20);
    private int selectedId = -1;

    public WarehousePanel(User currentUser) {
        super(currentUser, "Warehouses");
        initUI();
        loadData();
    }

    private void initUI() {
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Warehouse"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addFormRow(form, gbc, 0, "Name:", txtName);
        addFormRow(form, gbc, 1, "Type:", comboType);
        addFormRow(form, gbc, 2, "Province:", comboProvince);
        addFormRow(form, gbc, 3, "Parent:", comboParent);
        addFormRow(form, gbc, 4, "Location:", txtLocation);

        JButton btnSave = new JButton("Save");
        JButton btnNew = new JButton("New");
        JButton btnDelete = new JButton("Delete");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btns.add(btnSave);
        btns.add(btnNew);
        btns.add(btnDelete);
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        form.add(btns, gbc);
        add(form, BorderLayout.SOUTH);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                int r = table.convertRowIndexToModel(table.getSelectedRow());
                selectedId = (int) getModel(table).getValueAt(r, 0);
                txtName.setText((String) getModel(table).getValueAt(r, 1));
                setCombo(comboType, (String) getModel(table).getValueAt(r, 2));
                setCombo(comboProvince, (String) getModel(table).getValueAt(r, 3));
                String parent = (String) getModel(table).getValueAt(r, 4);
                setComboParent(parent);
                txtLocation.setText((String) getModel(table).getValueAt(r, 5));
            }
        });

        btnSave.addActionListener(e -> save());
        btnNew.addActionListener(e -> clearForm());
        btnDelete.addActionListener(e -> delete());
    }

    private <T> void setCombo(JComboBox<T> combo, String name) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).toString().equals(name)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void setComboParent(String name) {
        for (int i = 0; i < comboParent.getItemCount(); i++) {
            if (comboParent.getItemAt(i).toString().equals(name) ||
                    (name == null && comboParent.getItemAt(i).getWarehouseId() == -1)) {
                comboParent.setSelectedIndex(i);
                return;
            }
        }
    }

    private void loadData() {
        runBackground(() -> new Data(warehouseService.findAll(), provinceService.findAll(), typeService.findAll()),
                data -> {
                    comboType.removeAllItems();
                    for (WarehouseType t : data.types) comboType.addItem(t);

                    comboProvince.removeAllItems();
                    for (Province p : data.provinces) comboProvince.addItem(p);

                    comboParent.removeAllItems();
                    Warehouse none = new Warehouse();
                    none.setWarehouseId(-1);
                    none.setName("(none)");
                    comboParent.addItem(none);
                    for (Warehouse w : data.warehouses) {
                        if (w.getWarehouseId() != selectedId) {
                            comboParent.addItem(w);
                        }
                    }

                    var m = getModel(table);
                    m.setRowCount(0);
                    for (Warehouse w : data.warehouses) {
                        m.addRow(new Object[]{w.getWarehouseId(), w.getName(), w.getTypeName(),
                                w.getProvinceName(), w.getParentName(), w.getLocation()});
                    }
                    clearStatus();
                }, ex -> setStatus("Error: " + ex.getMessage(), true));
    }

    private void save() {
        String name = txtName.getText().trim();
        WarehouseType type = (WarehouseType) comboType.getSelectedItem();
        Province province = (Province) comboProvince.getSelectedItem();
        Warehouse parent = (Warehouse) comboParent.getSelectedItem();
        if (name.isEmpty() || type == null || province == null) {
            showError(this, "Name, type and province are required.");
            return;
        }

        Warehouse w = new Warehouse();
        w.setName(name);
        w.setTypeId(type.getTypeId());
        w.setProvinceId(province.getProvinceId());
        w.setLocation(txtLocation.getText().trim());
        if (parent != null && parent.getWarehouseId() > 0) {
            w.setParentWarehouseId(parent.getWarehouseId());
        } else {
            w.setParentWarehouseId(null);
        }

        if (selectedId <= 0) {
            runBackground(() -> warehouseService.create(currentUser, w),
                    created -> {
                        showInfo(this, "Warehouse created.");
                        clearForm();
                        loadData();
                    }, ex -> setStatus("Error: " + ex.getMessage(), true));
        } else {
            w.setWarehouseId(selectedId);
            runBackground(() -> {
                warehouseService.update(currentUser, w);
                return w;
            }, updated -> {
                showInfo(this, "Warehouse updated.");
                clearForm();
                loadData();
            }, ex -> setStatus("Error: " + ex.getMessage(), true));
        }
    }

    private void delete() {
        if (selectedId <= 0) return;
        if (!confirm(this, "Delete warehouse?")) return;
        runBackground(() -> {
            warehouseService.delete(currentUser, selectedId);
            return selectedId;
        }, id -> {
            showInfo(this, "Warehouse deleted.");
            clearForm();
            loadData();
        }, ex -> setStatus("Error: " + ex.getMessage(), true));
    }

    private void clearForm() {
        selectedId = -1;
        txtName.setText("");
        txtLocation.setText("");
        comboType.setSelectedIndex(0);
        comboProvince.setSelectedIndex(0);
        comboParent.setSelectedIndex(0);
        table.clearSelection();
    }

    private record Data(List<Warehouse> warehouses, List<Province> provinces, List<WarehouseType> types) {}
}
