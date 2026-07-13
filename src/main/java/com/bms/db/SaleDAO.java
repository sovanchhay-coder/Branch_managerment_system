package com.bms.db;

import com.bms.model.Sale;
import com.bms.model.SaleItem;
import com.bms.util.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SaleDAO {

    public List<Sale> findAll() throws SQLException {
        String sql = baseSql() + " ORDER BY s.sale_date DESC";
        return query(sql, null);
    }

    public Optional<Sale> findById(int id) throws SQLException {
        String sql = baseSql() + " WHERE s.sale_id = ?";
        List<Sale> list = query(sql, ps -> ps.setInt(1, id));
        if (list.isEmpty()) return Optional.empty();
        Sale sale = list.get(0);
        sale.setItems(findItemsBySaleId(id));
        return Optional.of(sale);
    }

    public List<Sale> findByUser(int userId) throws SQLException {
        String sql = baseSql() + " WHERE s.user_id = ? ORDER BY s.sale_date DESC";
        return query(sql, ps -> ps.setInt(1, userId));
    }

    public List<Sale> findByDateRange(LocalDateTime start, LocalDateTime end) throws SQLException {
        String sql = baseSql() + " WHERE s.sale_date BETWEEN ? AND ? ORDER BY s.sale_date DESC";
        return query(sql, ps -> {
            ps.setTimestamp(1, Timestamp.valueOf(start));
            ps.setTimestamp(2, Timestamp.valueOf(end));
        });
    }

    public List<Sale> findByUserAndDateRange(int userId, LocalDateTime start, LocalDateTime end) throws SQLException {
        String sql = baseSql() + " WHERE s.user_id = ? AND s.sale_date BETWEEN ? AND ? ORDER BY s.sale_date DESC";
        return query(sql, ps -> {
            ps.setInt(1, userId);
            ps.setTimestamp(2, Timestamp.valueOf(start));
            ps.setTimestamp(3, Timestamp.valueOf(end));
        });
    }

    public List<SaleItem> findItemsBySaleId(int saleId) throws SQLException {
        String sql = "SELECT item_id, sale_id, product_id, product_name, quantity, unit_price, subtotal " +
                     "FROM sale_items WHERE sale_id = ?";
        List<SaleItem> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, saleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapItem(rs));
                }
            }
        }
        return list;
    }

    public Sale insert(Sale sale) throws SQLException {
        String sql = "INSERT INTO sales (user_id, payment_method_id, total_amount) VALUES (?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, sale.getUserId());
            ps.setInt(2, sale.getPaymentMethodId());
            ps.setBigDecimal(3, sale.getTotalAmount());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    sale.setSaleId(rs.getInt(1));
                }
            }
        }
        return sale;
    }

    public void insertItem(SaleItem item) throws SQLException {
        String sql = "INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES (?,?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, item.getSaleId());
            ps.setInt(2, item.getProductId());
            ps.setInt(3, item.getQuantity());
            ps.setBigDecimal(4, item.getUnitPrice());
            ps.setBigDecimal(5, item.getSubtotal());
            ps.executeUpdate();
        }
    }

    public List<TopSellingRow> topSelling(LocalDateTime start, LocalDateTime end, int limit) throws SQLException {
        String sql = "SELECT si.product_id, p.product_name, SUM(si.quantity) as total_qty, SUM(si.subtotal) as total_revenue " +
                     "FROM sale_items si " +
                     "JOIN sales s ON si.sale_id = s.sale_id " +
                     "JOIN products p ON si.product_id = p.product_id " +
                     "WHERE s.sale_date BETWEEN ? AND ? " +
                     "GROUP BY si.product_id, p.product_name " +
                     "ORDER BY total_qty DESC LIMIT ?";
        List<TopSellingRow> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(start));
            ps.setTimestamp(2, Timestamp.valueOf(end));
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new TopSellingRow(
                            rs.getInt("product_id"),
                            rs.getString("product_name"),
                            rs.getInt("total_qty"),
                            rs.getBigDecimal("total_revenue")
                    ));
                }
            }
        }
        return list;
    }

    private String baseSql() {
        return "SELECT s.sale_id, s.user_id, u.username, s.payment_method_id, pm.method_name, s.sale_date, s.total_amount " +
               "FROM sales s " +
               "JOIN users u ON s.user_id = u.user_id " +
               "JOIN payment_methods pm ON s.payment_method_id = pm.payment_method_id";
    }

    @FunctionalInterface
    private interface ParamSetter {
        void set(PreparedStatement ps) throws SQLException;
    }

    private List<Sale> query(String sql, ParamSetter setter) throws SQLException {
        List<Sale> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (setter != null) setter.set(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    private Sale map(ResultSet rs) throws SQLException {
        Sale s = new Sale();
        s.setSaleId(rs.getInt("sale_id"));
        s.setUserId(rs.getInt("user_id"));
        s.setUsername(rs.getString("username"));
        s.setPaymentMethodId(rs.getInt("payment_method_id"));
        s.setPaymentMethodName(rs.getString("method_name"));
        s.setSaleDate(rs.getTimestamp("sale_date"));
        s.setTotalAmount(rs.getBigDecimal("total_amount"));
        return s;
    }

    private SaleItem mapItem(ResultSet rs) throws SQLException {
        SaleItem si = new SaleItem();
        si.setItemId(rs.getInt("item_id"));
        si.setSaleId(rs.getInt("sale_id"));
        si.setProductId(rs.getInt("product_id"));
        si.setProductName(rs.getString("product_name"));
        si.setQuantity(rs.getInt("quantity"));
        si.setUnitPrice(rs.getBigDecimal("unit_price"));
        si.setSubtotal(rs.getBigDecimal("subtotal"));
        return si;
    }

    public record TopSellingRow(int productId, String productName, int totalQty, BigDecimal totalRevenue) {}
}
