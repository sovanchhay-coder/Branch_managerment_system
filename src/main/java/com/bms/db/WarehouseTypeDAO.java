package com.bms.db;

import com.bms.model.WarehouseType;
import com.bms.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WarehouseTypeDAO {

    public List<WarehouseType> findAll() throws SQLException {
        String sql = "SELECT type_id, type_name FROM warehouse_types ORDER BY type_id";
        List<WarehouseType> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public Optional<WarehouseType> findById(int id) throws SQLException {
        String sql = "SELECT type_id, type_name FROM warehouse_types WHERE type_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    private WarehouseType map(ResultSet rs) throws SQLException {
        return new WarehouseType(rs.getInt("type_id"), rs.getString("type_name"));
    }
}
