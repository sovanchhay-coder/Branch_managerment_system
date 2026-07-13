package com.bms.ui.panels;

import com.bms.model.*;
import com.bms.service.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class POSPanel extends BasePanel {

    private final SaleService saleService = new SaleService();
    private final ProductService productService = new ProductService();
    private final PaymentMethodService paymentMethodService = new PaymentMethodService();
    private final InventoryService inventoryService = new InventoryService();

    private final JTextField txtSearch = new JTextField(20);
    private final JList<Product> listResults = new JList<>();
    private final DefaultListModel<Product> resultModel = new DefaultListModel<>();
    private final JTable cartTable = createTable(List.of("Product", "Qty", "Unit Price", "Subtotal"));
    private final DefaultTableModel cartModel = (DefaultTableModel) cartTable.getModel();
    private final JComboBox<PaymentMethod> comboPayment = new JComboBox<>();
    private final JLabel lblTotal = new JLabel("Total: $0.00");
    private final JTable historyTable = createTable(List.of("Sale ID", "Date", "Payment", "Total"));

    private final List<SaleItem> cart = new ArrayList<>();
    private int storeWarehouseId = -1;

    public POSPanel(User currentUser) {
        super(currentUser, "Point of Sale");
        initUI();
        loadPaymentMethods();
        resolveStoreWarehouse();
        loadHistory();
    }

    private void initUI() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Sell", buildSellTab());
        tabs.addTab("My Sales", buildHistoryTab());
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildSellTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search Product"));
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchRow.add(new JLabel("Name:"));
        searchRow.add(txtSearch);
        JButton btnSearch = new JButton("Search");
        searchRow.add(btnSearch);
        searchPanel.add(searchRow, BorderLayout.NORTH);

        listResults.setModel(resultModel);
        listResults.setVisibleRowCount(5);
        searchPanel.add(new JScrollPane(listResults), BorderLayout.CENTER);

        JButton btnAdd = new JButton("Add to Cart");
        searchPanel.add(btnAdd, BorderLayout.SOUTH);
        panel.add(searchPanel, BorderLayout.NORTH);

        JPanel cartPanel = new JPanel(new BorderLayout(5, 5));
        cartPanel.setBorder(BorderFactory.createTitledBorder("Cart"));
        cartPanel.add(new JScrollPane(cartTable), BorderLayout.CENTER);

        JPanel cartBottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnRemove = new JButton("Remove Selected");
        cartBottom.add(btnRemove);
        cartPanel.add(cartBottom, BorderLayout.SOUTH);
        panel.add(cartPanel, BorderLayout.CENTER);

        JPanel checkout = new JPanel(new GridBagLayout());
        checkout.setBorder(BorderFactory.createTitledBorder("Checkout"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0; gbc.gridy = 0;
        checkout.add(new JLabel("Payment:"), gbc);
        gbc.gridx = 1;
        checkout.add(comboPayment, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        lblTotal.setFont(lblTotal.getFont().deriveFont(Font.BOLD, 16f));
        checkout.add(lblTotal, gbc);
        gbc.gridy = 2;
        JButton btnSubmit = new JButton("Submit Sale");
        checkout.add(btnSubmit, gbc);
        panel.add(checkout, BorderLayout.SOUTH);

        btnSearch.addActionListener(e -> doSearch());
        txtSearch.addActionListener(e -> doSearch());
        btnAdd.addActionListener(e -> addToCart());
        btnRemove.addActionListener(e -> removeFromCart());
        btnSubmit.addActionListener(e -> submitSale());

        return panel;
    }

    private JPanel buildHistoryTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(e -> loadHistory());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(btnRefresh);
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(historyTable), BorderLayout.CENTER);
        return panel;
    }

    private void loadPaymentMethods() {
        runBackground(paymentMethodService::findAll,
                list -> {
                    comboPayment.removeAllItems();
                    for (PaymentMethod pm : list) comboPayment.addItem(pm);
                }, ex -> setStatus("Error loading payments: " + ex.getMessage(), true));
    }

    private void resolveStoreWarehouse() {
        runBackground(() -> {
            if (currentUser.getBranchId() == null) return -1;
            Branch b = new com.bms.db.BranchDAO().findById(currentUser.getBranchId()).orElse(null);
            return (b != null && b.getWarehouseId() != null) ? b.getWarehouseId() : -1;
        }, id -> storeWarehouseId = id,
                ex -> setStatus("Could not resolve store warehouse: " + ex.getMessage(), true));
    }

    private void doSearch() {
        String term = txtSearch.getText().trim();
        if (term.isEmpty()) return;
        runBackground(() -> productService.searchByName(term),
                list -> {
                    resultModel.clear();
                    for (Product p : list) resultModel.addElement(p);
                }, ex -> setStatus("Search error: " + ex.getMessage(), true));
    }

    private void addToCart() {
        Product p = listResults.getSelectedValue();
        if (p == null) {
            showError(this, "Select a product from search results.");
            return;
        }
        String input = JOptionPane.showInputDialog(this, "Quantity for " + p.getProductName() + ":", "1");
        if (input == null) return;
        int qty;
        try {
            qty = Integer.parseInt(input.trim());
        } catch (NumberFormatException ex) {
            showError(this, "Invalid quantity.");
            return;
        }
        if (qty <= 0) return;

        runBackground(() -> inventoryService.getAvailable(p.getProductId(), storeWarehouseId),
                available -> {
                    if (qty > available) {
                        showError(this, "Only " + available + " available.");
                        return;
                    }
                    SaleItem item = new SaleItem(p.getProductId(), p.getProductName(), qty, p.getPrice());
                    cart.add(item);
                    cartModel.addRow(new Object[]{item.getProductName(), item.getQuantity(), item.getUnitPrice(), item.getSubtotal()});
                    updateTotal();
                    txtSearch.setText("");
                    resultModel.clear();
                }, ex -> setStatus("Stock check error: " + ex.getMessage(), true));
    }

    private void removeFromCart() {
        int row = cartTable.getSelectedRow();
        if (row < 0) return;
        cart.remove(row);
        cartModel.removeRow(row);
        updateTotal();
    }

    private void updateTotal() {
        BigDecimal total = cart.stream()
                .map(SaleItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotal.setText("Total: $" + total);
    }

    private void submitSale() {
        if (cart.isEmpty()) {
            showError(this, "Cart is empty.");
            return;
        }
        PaymentMethod pm = (PaymentMethod) comboPayment.getSelectedItem();
        if (pm == null) {
            showError(this, "Select a payment method.");
            return;
        }
        if (!confirm(this, "Submit sale for " + lblTotal.getText() + "?")) return;

        List<SaleItem> items = new ArrayList<>(cart);
        runBackground(() -> saleService.processSale(currentUser, pm.getPaymentMethodId(), items),
                sale -> {
                    showInfo(this, "Sale completed. Sale ID: " + sale.getSaleId());
                    cart.clear();
                    cartModel.setRowCount(0);
                    updateTotal();
                    loadHistory();
                }, ex -> setStatus("Sale error: " + ex.getMessage(), true));
    }

    private void loadHistory() {
        runBackground(() -> saleService.findByUser(currentUser.getUserId()),
                list -> {
                    DefaultTableModel m = getModel(historyTable);
                    m.setRowCount(0);
                    for (Sale s : list) {
                        m.addRow(new Object[]{s.getSaleId(), s.getSaleDate(), s.getPaymentMethodName(), s.getTotalAmount()});
                    }
                }, ex -> setStatus("History error: " + ex.getMessage(), true));
    }
}
