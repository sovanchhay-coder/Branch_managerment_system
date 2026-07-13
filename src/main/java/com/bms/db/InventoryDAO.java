package com.bms.db;

import com.bms.model.Inventory;
import com.bms.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InventoryDAO {

    public List<Inventory> findAll() throws SQLException {
        String sql = baseSql() + " ORDER BY w.name, p.product_name";
        return query(sql, null);
    }

    public List<Inventory> findByWarehouse(int warehouseId) throws SQLException {
        String sql = baseSql() + " WHERE i.warehouse_id = ? ORDER BY p.product_name";
        return query(sql, ps -> ps.setInt(1, warehouseId));
    }

    public List<Inventory> findByProduct(int productId) throws SQLException {
        String sql = baseSql() + " WHERE i.product_id = ? ORDER BY w.name";
        return query(sql, ps -> ps.setInt(1, productId));
    }

    public Optional<Inventory> findByProductAndWarehouse(int productId, int warehouseId) throws SQLException {
        String sql = baseSql() + " WHERE i.product_id = ? AND i.warehouse_id = ?";
        List<Inventory> list = query(sql, ps -> {
            ps.setInt(1, productId);
            ps.setInt(2, warehouseId);
        });
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public int getQuantity(int productId, int warehouseId) throws SQLException {
        String sql = "SELECT quantity FROM inventory WHERE product_id = ? AND warehouse_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setInt(2, warehouseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("quantity");
                }
            }
        }
        return 0;
    }

    public int adjustQuantity(Connection conn, int productId, int warehouseId, int delta) throws SQLException {
        int current = 0;
        String select = "SELECT inventory_id, quantity FROM inventory WHERE product_id = ? AND warehouse_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(select)) {
            ps.setInt(1, productId);
            ps.setInt(2, warehouseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    current = rs.getInt("quantity");
                }
            }
        }

        int next = current + delta;
        if (next < 0) {
            throw new SQLException("Insufficient stock for product " + productId + " at warehouse " + warehouseId);
        }

        if (current == 0) {
            String insert = "INSERT INTO inventory (product_id, warehouse_id, quantity) VALUES (?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(insert)) {
                ps.setInt(1, productId);
                ps.setInt(2, warehouseId);
                ps.setInt(3, next);
                ps.executeUpdate();
            }
        } else {
            String update = "UPDATE inventory SET quantity = ? WHERE product_id = ? AND warehouse_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(update)) {
                ps.setInt(1, next);
                ps.setInt(2, productId);
                ps.setInt(3, warehouseId);
                ps.executeUpdate();
            }
        }
        return next;
    }

    public int adjustQuantity(int productId, int warehouseId, int delta) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int result = adjustQuantity(conn, productId, warehouseId, delta);
                conn.commit();
                return result;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void setQuantity(int productId, int warehouseId, int quantity) throws SQLException {
        String sql = "INSERT INTO inventory (product_id, warehouse_id, quantity) VALUES (?,?,?) " +
                     "ON DUPLICATE KEY UPDATE quantity = VALUES(quantity)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setInt(2, warehouseId);
            ps.setInt(3, quantity);
            ps.executeUpdate();
        }
    }

    private String baseSql() {
        return "SELECT i.inventory_id, i.product_id, p.product_name, i.warehouse_id, w.name as warehouse_name, " +
               "i.quantity, i.last_updated " +
               "FROM inventory i " +
               "JOIN products p ON i.product_id = p.product_id " +
               "JOIN warehouses w ON i.warehouse_id = w.warehouse_id";
    }

    @FunctionalInterface
    private interface ParamSetter {
        void set(PreparedStatement ps) throws SQLException;
    }

    private List<Inventory> query(String sql, ParamSetter setter) throws SQLException {
        List<Inventory> list = new ArrayList<>();
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

    private Inventory map(ResultSet rs) throws SQLException {
        Inventory i = new Inventory();
        i.setInventoryId(rs.getInt("inventory_id"));
        i.setProductId(rs.getInt("product_id"));
        i.setProductName(rs.getString("product_name"));
        i.setWarehouseId(rs.getInt("warehouse_id"));
        i.setWarehouseName(rs.getString("warehouse_name"));
        i.setQuantity(rs.getInt("quantity"));
        i.setLastUpdated(rs.getTimestamp("last_updated"));
        return i;
    }
}
