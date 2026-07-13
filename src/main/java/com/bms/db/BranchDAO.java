package com.bms.db;

import com.bms.model.Branch;
import com.bms.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BranchDAO {

    public List<Branch> findAll() throws SQLException {
        String sql = baseSql() + " ORDER BY b.branch_name";
        return query(sql, null);
    }

    public Optional<Branch> findById(int id) throws SQLException {
        String sql = baseSql() + " WHERE b.branch_id = ?";
        List<Branch> list = query(sql, ps -> ps.setInt(1, id));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<Branch> findByProvince(int provinceId) throws SQLException {
        String sql = baseSql() + " WHERE b.province_id = ? ORDER BY b.branch_name";
        return query(sql, ps -> ps.setInt(1, provinceId));
    }

    public List<Branch> findByWarehouse(int warehouseId) throws SQLException {
        String sql = baseSql() + " WHERE b.warehouse_id = ? ORDER BY b.branch_name";
        return query(sql, ps -> ps.setInt(1, warehouseId));
    }

    public Branch insert(Branch b) throws SQLException {
        String sql = "INSERT INTO branches (branch_name, warehouse_id, province_id, address) VALUES (?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, b.getBranchName());
            ps.setObject(2, b.getWarehouseId(), Types.INTEGER);
            ps.setObject(3, b.getProvinceId(), Types.INTEGER);
            ps.setString(4, b.getAddress());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    b.setBranchId(rs.getInt(1));
                }
            }
        }
        return b;
    }

    public void update(Branch b) throws SQLException {
        String sql = "UPDATE branches SET branch_name=?, warehouse_id=?, province_id=?, address=? WHERE branch_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, b.getBranchName());
            ps.setObject(2, b.getWarehouseId(), Types.INTEGER);
            ps.setObject(3, b.getProvinceId(), Types.INTEGER);
            ps.setString(4, b.getAddress());
            ps.setInt(5, b.getBranchId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM branches WHERE branch_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private String baseSql() {
        return "SELECT b.branch_id, b.branch_name, b.warehouse_id, w.name as warehouse_name, " +
               "b.province_id, p.province_name, b.address " +
               "FROM branches b " +
               "LEFT JOIN warehouses w ON b.warehouse_id = w.warehouse_id " +
               "LEFT JOIN provinces p ON b.province_id = p.province_id";
    }

    @FunctionalInterface
    private interface ParamSetter {
        void set(PreparedStatement ps) throws SQLException;
    }

    private List<Branch> query(String sql, ParamSetter setter) throws SQLException {
        List<Branch> list = new ArrayList<>();
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

    private Branch map(ResultSet rs) throws SQLException {
        Branch b = new Branch();
        b.setBranchId(rs.getInt("branch_id"));
        b.setBranchName(rs.getString("branch_name"));
        b.setWarehouseId((Integer) rs.getObject("warehouse_id"));
        b.setWarehouseName(rs.getString("warehouse_name"));
        b.setProvinceId((Integer) rs.getObject("province_id"));
        b.setProvinceName(rs.getString("province_name"));
        b.setAddress(rs.getString("address"));
        return b;
    }
}
