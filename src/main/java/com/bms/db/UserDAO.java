package com.bms.db;

import com.bms.model.User;
import com.bms.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO {

    public List<User> findAll() throws SQLException {
        String sql = baseSql() + " ORDER BY u.username";
        return query(sql, null);
    }

    public Optional<User> findById(int id) throws SQLException {
        String sql = baseSql() + " WHERE u.user_id = ?";
        List<User> list = query(sql, ps -> ps.setInt(1, id));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = baseSql() + " WHERE u.username = ?";
        List<User> list = query(sql, ps -> ps.setString(1, username));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public User insert(User u) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, role_id, branch_id, warehouse_id) VALUES (?,?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPasswordHash());
            ps.setInt(3, u.getRoleId());
            ps.setObject(4, u.getBranchId(), Types.INTEGER);
            ps.setObject(5, u.getWarehouseId(), Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    u.setUserId(rs.getInt(1));
                }
            }
        }
        return u;
    }

    public void update(User u) throws SQLException {
        String sql = "UPDATE users SET username=?, role_id=?, branch_id=?, warehouse_id=? WHERE user_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getUsername());
            ps.setInt(2, u.getRoleId());
            ps.setObject(3, u.getBranchId(), Types.INTEGER);
            ps.setObject(4, u.getWarehouseId(), Types.INTEGER);
            ps.setInt(5, u.getUserId());
            ps.executeUpdate();
        }
    }

    public void updatePassword(int userId, String hash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private String baseSql() {
        return "SELECT u.user_id, u.username, u.password_hash, u.role_id, r.role_name, r.level, " +
               "u.branch_id, b.branch_name, u.warehouse_id, w.name as warehouse_name, u.created_at " +
               "FROM users u " +
               "JOIN roles r ON u.role_id = r.role_id " +
               "LEFT JOIN branches b ON u.branch_id = b.branch_id " +
               "LEFT JOIN warehouses w ON u.warehouse_id = w.warehouse_id";
    }

    @FunctionalInterface
    private interface ParamSetter {
        void set(PreparedStatement ps) throws SQLException;
    }

    private List<User> query(String sql, ParamSetter setter) throws SQLException {
        List<User> list = new ArrayList<>();
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

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRoleId(rs.getInt("role_id"));
        u.setRoleName(rs.getString("role_name"));
        u.setRoleLevel(rs.getInt("level"));
        u.setBranchId((Integer) rs.getObject("branch_id"));
        u.setBranchName(rs.getString("branch_name"));
        u.setWarehouseId((Integer) rs.getObject("warehouse_id"));
        u.setWarehouseName(rs.getString("warehouse_name"));
        u.setCreatedAt(rs.getTimestamp("created_at"));
        return u;
    }
}
