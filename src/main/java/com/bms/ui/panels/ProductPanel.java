package com.bms.ui.panels;

import com.bms.model.Category;
import com.bms.model.Product;
import com.bms.model.User;
import com.bms.service.CategoryService;
import com.bms.service.ProductService;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

public class ProductPanel extends BasePanel {

    private final ProductService productService = new ProductService();
    private final CategoryService categoryService = new CategoryService();

    private final JTable table = createTable(List.of("ID", "Name", "Category", "Price", "Description"));
    private final JTextField txtName = new JTextField(20);
    private final JComboBox<Category> comboCategory = new JComboBox<>();
    private final JTextField txtPrice = new JTextField(10);
    private final JTextField txtDescription = new JTextField(30);
    private int selectedId = -1;

    public ProductPanel(User currentUser) {
        super(currentUser, "Products");
        initUI();
        loadData();
    }

    private void initUI() {
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Product Details"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addFormRow(form, gbc, 0, "Name:", txtName);
        addFormRow(form, gbc, 1, "Category:", comboCategory);
        addFormRow(form, gbc, 2, "Price:", txtPrice);
        addFormRow(form, gbc, 3, "Description:", txtDescription);

        JButton btnSave = new JButton("Save");
        JButton btnNew = new JButton("New");
        JButton btnDelete = new JButton("Delete");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btns.add(btnSave);
        btns.add(btnNew);
        btns.add(btnDelete);
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        form.add(btns, gbc);
        add(form, BorderLayout.SOUTH);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                int modelRow = table.convertRowIndexToModel(table.getSelectedRow());
                selectedId = (int) getModel(table).getValueAt(modelRow, 0);
                txtName.setText((String) getModel(table).getValueAt(modelRow, 1));
                setCategory((String) getModel(table).getValueAt(modelRow, 2));
                txtPrice.setText(getModel(table).getValueAt(modelRow, 3).toString());
                txtDescription.setText((String) getModel(table).getValueAt(modelRow, 4));
            }
        });

        btnSave.addActionListener(e -> save());
        btnNew.addActionListener(e -> clearForm());
        btnDelete.addActionListener(e -> delete());
    }

    private void setCategory(String name) {
        for (int i = 0; i < comboCategory.getItemCount(); i++) {
            if (comboCategory.getItemAt(i).getCategoryName().equals(name)) {
                comboCategory.setSelectedIndex(i);
                return;
            }
        }
    }

    private void loadData() {
        runBackground(() -> new Data(productService.findAll(), categoryService.findAll()),
                data -> {
                    comboCategory.removeAllItems();
                    for (Category c : data.categories) comboCategory.addItem(c);

                    var m = getModel(table);
                    m.setRowCount(0);
                    for (Product p : data.products) {
                        m.addRow(new Object[]{p.getProductId(), p.getProductName(), p.getCategoryName(), p.getPrice(), p.getDescription()});
                    }
                    clearStatus();
                }, ex -> setStatus("Error: " + ex.getMessage(), true));
    }

    private void save() {
        String name = txtName.getText().trim();
        Category cat = (Category) comboCategory.getSelectedItem();
        String priceStr = txtPrice.getText().trim();
        if (name.isEmpty() || cat == null || priceStr.isEmpty()) {
            showError(this, "Fill in all required fields.");
            return;
        }
        BigDecimal price;
        try {
            price = new BigDecimal(priceStr);
        } catch (NumberFormatException ex) {
            showError(this, "Invalid price.");
            return;
        }

        Product p = new Product();
        p.setProductName(name);
        p.setCategoryId(cat.getCategoryId());
        p.setPrice(price);
        p.setDescription(txtDescription.getText().trim());

        if (selectedId <= 0) {
            runBackground(() -> productService.create(currentUser, p),
                    created -> {
                        showInfo(this, "Product created.");
                        clearForm();
                        loadData();
                    }, ex -> setStatus("Error: " + ex.getMessage(), true));
        } else {
            p.setProductId(selectedId);
            runBackground(() -> {
                productService.update(currentUser, p);
                return p;
            }, updated -> {
                showInfo(this, "Product updated.");
                clearForm();
                loadData();
            }, ex -> setStatus("Error: " + ex.getMessage(), true));
        }
    }

    private void delete() {
        if (selectedId <= 0) {
            showError(this, "Select a product to delete.");
            return;
        }
        if (!confirm(this, "Delete this product?")) return;
        runBackground(() -> {
            productService.delete(currentUser, selectedId);
            return selectedId;
        }, id -> {
            showInfo(this, "Product deleted.");
            clearForm();
            loadData();
        }, ex -> setStatus("Error: " + ex.getMessage(), true));
    }

    private void clearForm() {
        selectedId = -1;
        txtName.setText("");
        txtPrice.setText("");
        txtDescription.setText("");
        comboCategory.setSelectedIndex(0);
        table.clearSelection();
    }

    private record Data(List<Product> products, List<Category> categories) {}
}
