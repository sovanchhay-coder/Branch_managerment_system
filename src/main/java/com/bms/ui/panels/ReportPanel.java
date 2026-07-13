package com.bms.ui.panels;

import com.bms.db.SaleDAO;
import com.bms.model.*;
import com.bms.service.ReportService;
import com.github.lgooddatepicker.components.DatePicker;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class ReportPanel extends BasePanel {

    private final ReportService reportService = new ReportService();
    private final DatePicker dpFrom = new DatePicker();
    private final DatePicker dpTo = new DatePicker();
    private final JTable table = createTable(List.of("Col1", "Col2", "Col3", "Col4", "Col5"));
    private String currentMode = "";

    public ReportPanel(User currentUser) {
        super(currentUser, "Reports");
        initUI();
    }

    private void initUI() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("From:"));
        top.add(dpFrom);
        top.add(new JLabel("To:"));
        top.add(dpTo);

        dpFrom.setDate(LocalDate.now().minusMonths(1));
        dpTo.setDate(LocalDate.now());

        JButton btnSales = new JButton("Sales by Date");
        JButton btnTop = new JButton("Top Selling");
        JButton btnInventory = new JButton("Current Inventory");
        top.add(btnSales);
        top.add(btnTop);
        top.add(btnInventory);
        add(top, BorderLayout.NORTH);

        add(new JScrollPane(table), BorderLayout.CENTER);

        btnSales.addActionListener(e -> loadSales());
        btnTop.addActionListener(e -> loadTopSelling());
        btnInventory.addActionListener(e -> loadInventory());
    }

    private LocalDateTime startOfDay(LocalDate d) {
        return d != null ? d.atStartOfDay() : LocalDateTime.now().minusYears(10);
    }

    private LocalDateTime endOfDay(LocalDate d) {
        return d != null ? d.atTime(LocalTime.MAX) : LocalDateTime.now();
    }

    private void loadSales() {
        runBackground(() -> reportService.salesByDateRange(currentUser, startOfDay(dpFrom.getDate()), endOfDay(dpTo.getDate())),
                list -> {
                    currentMode = "sales";
                    DefaultTableModel m = new DefaultTableModel(
                            new String[]{"Sale ID", "User", "Payment", "Date", "Total"}, 0) {
                        @Override public boolean isCellEditable(int row, int column) { return false; }
                    };
                    for (Sale s : list) {
                        m.addRow(new Object[]{s.getSaleId(), s.getUsername(), s.getPaymentMethodName(), s.getSaleDate(), s.getTotalAmount()});
                    }
                    table.setModel(m);
                    setStatus("Found " + list.size() + " sales.", false);
                }, ex -> setStatus("Error: " + ex.getMessage(), true));
    }

    private void loadTopSelling() {
        runBackground(() -> reportService.topSelling(currentUser, startOfDay(dpFrom.getDate()), endOfDay(dpTo.getDate()), 20),
                list -> {
                    currentMode = "top";
                    DefaultTableModel m = new DefaultTableModel(
                            new String[]{"Product ID", "Product", "Qty Sold", "Revenue"}, 0) {
                        @Override public boolean isCellEditable(int row, int column) { return false; }
                    };
                    for (SaleDAO.TopSellingRow row : list) {
                        m.addRow(new Object[]{row.productId(), row.productName(), row.totalQty(), row.totalRevenue()});
                    }
                    table.setModel(m);
                    setStatus("Top selling products loaded.", false);
                }, ex -> setStatus("Error: " + ex.getMessage(), true));
    }

    private void loadInventory() {
        runBackground(() -> reportService.currentInventory(currentUser),
                list -> {
                    currentMode = "inventory";
                    DefaultTableModel m = new DefaultTableModel(
                            new String[]{"ID", "Product", "Warehouse", "Qty", "Last Updated"}, 0) {
                        @Override public boolean isCellEditable(int row, int column) { return false; }
                    };
                    for (Inventory i : list) {
                        m.addRow(new Object[]{i.getInventoryId(), i.getProductName(), i.getWarehouseName(), i.getQuantity(), i.getLastUpdated()});
                    }
                    table.setModel(m);
                    setStatus("Current inventory loaded.", false);
                }, ex -> setStatus("Error: " + ex.getMessage(), true));
    }
}
