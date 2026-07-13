package com.bms.ui.panels;

import com.bms.model.Category;
import com.bms.model.User;
import com.bms.service.CategoryService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ProvinceCategoryPanel extends BasePanel {

    private final CategoryService categoryService = new CategoryService();
    private final JTable table = createTable(List.of("ID", "Category Name"));
    private final JTextField txtName = new JTextField(20);
    private int selectedId = -1;

    public ProvinceCategoryPanel(User currentUser) {
        super(currentUser, "Categories");
        initUI();
        loadData();
    }

    private void initUI() {
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Category"));
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
        runBackground(categoryService::findAll, list -> {
            var m = getModel(table);
            m.setRowCount(0);
            for (Category c : list) {
                m.addRow(new Object[]{c.getCategoryId(), c.getCategoryName()});
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
        Category c = new Category();
        c.setCategoryName(name);
        if (selectedId <= 0) {
            runBackground(() -> categoryService.create(currentUser, c),
                    created -> {
                        showInfo(this, "Category created.");
                        clearForm();
                        loadData();
                    }, ex -> setStatus("Error: " + ex.getMessage(), true));
        } else {
            c.setCategoryId(selectedId);
            runBackground(() -> {
                categoryService.update(currentUser, c);
                return c;
            }, updated -> {
                showInfo(this, "Category updated.");
                clearForm();
                loadData();
            }, ex -> setStatus("Error: " + ex.getMessage(), true));
        }
    }

    private void delete() {
        if (selectedId <= 0) return;
        if (!confirm(this, "Delete category?")) return;
        runBackground(() -> {
            categoryService.delete(currentUser, selectedId);
            return selectedId;
        }, id -> {
            showInfo(this, "Category deleted.");
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
