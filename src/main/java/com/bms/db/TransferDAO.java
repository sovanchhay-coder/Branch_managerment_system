package com.bms.db;

import com.bms.model.Transfer;
import com.bms.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TransferDAO {

    public List<Transfer> findAll() throws SQLException {
        String sql = baseSql() + " ORDER BY t.transfer_date DESC";
        return query(sql, null);
    }

    public List<Transfer> findPending() throws SQLException {
        String sql = baseSql() + " WHERE t.approved_by IS NULL ORDER BY t.transfer_date DESC";
        return query(sql, null);
    }

    public List<Transfer> findByWarehouse(int warehouseId) throws SQLException {
        String sql = baseSql() + " WHERE t.from_warehouse_id = ? OR t.to_warehouse_id = ? ORDER BY t.transfer_date DESC";
        return query(sql, ps -> {
            ps.setInt(1, warehouseId);
            ps.setInt(2, warehouseId);
        });
    }

    public Optional<Transfer> findById(int id) throws SQLException {
        String sql = baseSql() + " WHERE t.transfer_id = ?";
        List<Transfer> list = query(sql, ps -> ps.setInt(1, id));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Transfer insert(Transfer t) throws SQLException {
        String sql = "INSERT INTO transfers (from_warehouse_id, to_warehouse_id, product_id, quantity, approved_by) VALUES (?,?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, t.getFromWarehouseId());
            ps.setInt(2, t.getToWarehouseId());
            ps.setInt(3, t.getProductId());
            ps.setInt(4, t.getQuantity());
            ps.setObject(5, t.getApprovedBy(), Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    t.setTransferId(rs.getInt(1));
                }
            }
        }
        return t;
    }

    public void approve(int transferId, int approvedByUserId) throws SQLException {
        String sql = "UPDATE transfers SET approved_by = ? WHERE transfer_id = ? AND approved_by IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, approvedByUserId);
            ps.setInt(2, transferId);
            ps.executeUpdate();
        }
    }

    private String baseSql() {
        return "SELECT t.transfer_id, t.from_warehouse_id, fw.name as from_name, " +
               "t.to_warehouse_id, tw.name as to_name, t.product_id, p.product_name, " +
               "t.quantity, t.transfer_date, t.approved_by, u.username as approved_by_name " +
               "FROM transfers t " +
               "JOIN warehouses fw ON t.from_warehouse_id = fw.warehouse_id " +
               "JOIN warehouses tw ON t.to_warehouse_id = tw.warehouse_id " +
               "JOIN products p ON t.product_id = p.product_id " +
               "LEFT JOIN users u ON t.approved_by = u.user_id";
    }

    @FunctionalInterface
    private interface ParamSetter {
        void set(PreparedStatement ps) throws SQLException;
    }

    private List<Transfer> query(String sql, ParamSetter setter) throws SQLException {
        List<Transfer> list = new ArrayList<>();
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

    private Transfer map(ResultSet rs) throws SQLException {
        Transfer t = new Transfer();
        t.setTransferId(rs.getInt("transfer_id"));
        t.setFromWarehouseId(rs.getInt("from_warehouse_id"));
        t.setFromWarehouseName(rs.getString("from_name"));
        t.setToWarehouseId(rs.getInt("to_warehouse_id"));
        t.setToWarehouseName(rs.getString("to_name"));
        t.setProductId(rs.getInt("product_id"));
        t.setProductName(rs.getString("product_name"));
        t.setQuantity(rs.getInt("quantity"));
        t.setTransferDate(rs.getTimestamp("transfer_date"));
        t.setApprovedBy((Integer) rs.getObject("approved_by"));
        t.setApprovedByName(rs.getString("approved_by_name"));
        return t;
    }
}
