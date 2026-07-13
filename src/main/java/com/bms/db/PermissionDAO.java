package com.bms.db;

import com.bms.model.Permission;
import com.bms.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PermissionDAO {

    public List<Permission> findByRoleId(int roleId) throws SQLException {
        String sql = "SELECT p.permission_id, p.permission_name, p.description " +
                     "FROM permissions p JOIN role_permissions rp ON p.permission_id = rp.permission_id " +
                     "WHERE rp.role_id = ? ORDER BY p.permission_id";
        List<Permission> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public List<String> findNamesByRoleId(int roleId) throws SQLException {
        String sql = "SELECT p.permission_name " +
                     "FROM permissions p JOIN role_permissions rp ON p.permission_id = rp.permission_id " +
                     "WHERE rp.role_id = ? ORDER BY p.permission_id";
        List<String> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("permission_name"));
                }
            }
        }
        return list;
    }

    private Permission map(ResultSet rs) throws SQLException {
        return new Permission(
                rs.getInt("permission_id"),
                rs.getString("permission_name"),
                rs.getString("description")
        );
    }
}
