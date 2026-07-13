package com.bms.db;

import com.bms.model.Role;
import com.bms.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoleDAO {

    public List<Role> findAll() throws SQLException {
        String sql = "SELECT role_id, role_name, parent_role_id, level FROM roles ORDER BY level, role_id";
        List<Role> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public Optional<Role> findById(int id) throws SQLException {
        String sql = "SELECT role_id, role_name, parent_role_id, level FROM roles WHERE role_id = ?";
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

    private Role map(ResultSet rs) throws SQLException {
        return new Role(
                rs.getInt("role_id"),
                rs.getString("role_name"),
                (Integer) rs.getObject("parent_role_id"),
                rs.getInt("level")
        );
    }
}
