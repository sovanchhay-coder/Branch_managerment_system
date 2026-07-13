package com.bms.db;

import com.bms.model.Warehouse;
import com.bms.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WarehouseDAO {

    public List<Warehouse> findAll() throws SQLException {
        String sql = baseSql() + " ORDER BY w.warehouse_id";
        return query(sql, null);
    }

    public Optional<Warehouse> findById(int id) throws SQLException {
        String sql = baseSql() + " WHERE w.warehouse_id = ?";
        List<Warehouse> list = query(sql, ps -> ps.setInt(1, id));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<Warehouse> findByType(int typeId) throws SQLException {
        String sql = baseSql() + " WHERE w.type_id = ? ORDER BY w.name";
        return query(sql, ps -> ps.setInt(1, typeId));
    }

    public List<Warehouse> findByProvince(int provinceId) throws SQLException {
        String sql = baseSql() + " WHERE w.province_id = ? ORDER BY w.name";
        return query(sql, ps -> ps.setInt(1, provinceId));
    }

    public List<Warehouse> findByProvinceAndType(int provinceId, int typeId) throws SQLException {
        String sql = baseSql() + " WHERE w.province_id = ? AND w.type_id = ? ORDER BY w.name";
        return query(sql, ps -> {
            ps.setInt(1, provinceId);
            ps.setInt(2, typeId);
        });
    }

    public List<Warehouse> findChildren(int parentId) throws SQLException {
        String sql = baseSql() + " WHERE w.parent_warehouse_id = ? ORDER BY w.name";
        return query(sql, ps -> ps.setInt(1, parentId));
    }

    public Warehouse insert(Warehouse w) throws SQLException {
        String sql = "INSERT INTO warehouses (name, type_id, location, province_id, parent_warehouse_id) VALUES (?,?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, w.getName());
            ps.setInt(2, w.getTypeId());
            ps.setString(3, w.getLocation());
            ps.setInt(4, w.getProvinceId());
            ps.setObject(5, w.getParentWarehouseId(), Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    w.setWarehouseId(rs.getInt(1));
                }
            }
        }
        return w;
    }

    public void update(Warehouse w) throws SQLException {
        String sql = "UPDATE warehouses SET name=?, type_id=?, location=?, province_id=?, parent_warehouse_id=? WHERE warehouse_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, w.getName());
            ps.setInt(2, w.getTypeId());
            ps.setString(3, w.getLocation());
            ps.setInt(4, w.getProvinceId());
            ps.setObject(5, w.getParentWarehouseId(), Types.INTEGER);
            ps.setInt(6, w.getWarehouseId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM warehouses WHERE warehouse_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private String baseSql() {
        return "SELECT w.warehouse_id, w.name, w.type_id, wt.type_name, w.location, " +
               "w.province_id, p.province_name, w.parent_warehouse_id, pw.name as parent_name " +
               "FROM warehouses w " +
               "JOIN warehouse_types wt ON w.type_id = wt.type_id " +
               "JOIN provinces p ON w.province_id = p.province_id " +
               "LEFT JOIN warehouses pw ON w.parent_warehouse_id = pw.warehouse_id";
    }

    @FunctionalInterface
    private interface ParamSetter {
        void set(PreparedStatement ps) throws SQLException;
    }

    private List<Warehouse> query(String sql, ParamSetter setter) throws SQLException {
        List<Warehouse> list = new ArrayList<>();
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

    private Warehouse map(ResultSet rs) throws SQLException {
        Warehouse w = new Warehouse();
        w.setWarehouseId(rs.getInt("warehouse_id"));
        w.setName(rs.getString("name"));
        w.setTypeId(rs.getInt("type_id"));
        w.setTypeName(rs.getString("type_name"));
        w.setLocation(rs.getString("location"));
        w.setProvinceId(rs.getInt("province_id"));
        w.setProvinceName(rs.getString("province_name"));
        w.setParentWarehouseId((Integer) rs.getObject("parent_warehouse_id"));
        w.setParentName(rs.getString("parent_name"));
        return w;
    }
}
