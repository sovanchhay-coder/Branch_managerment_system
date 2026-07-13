package com.bms.db;

import com.bms.model.AuditLog;
import com.bms.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDAO {

    public AuditLog insert(AuditLog log) throws SQLException {
        String sql = "INSERT INTO audit_logs (user_id, action, table_name, record_id, details) VALUES (?,?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, log.getUserId(), Types.INTEGER);
            ps.setString(2, log.getAction());
            ps.setString(3, log.getTableName());
            ps.setObject(4, log.getRecordId(), Types.INTEGER);
            ps.setString(5, log.getDetails());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    log.setLogId(rs.getInt(1));
                }
            }
        }
        return log;
    }

    public List<AuditLog> findAll() throws SQLException {
        String sql = "SELECT log_id, user_id, action, table_name, record_id, action_date, details " +
                     "FROM audit_logs ORDER BY action_date DESC";
        List<AuditLog> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    private AuditLog map(ResultSet rs) throws SQLException {
        AuditLog log = new AuditLog();
        log.setLogId(rs.getInt("log_id"));
        log.setUserId((Integer) rs.getObject("user_id"));
        log.setAction(rs.getString("action"));
        log.setTableName(rs.getString("table_name"));
        log.setRecordId((Integer) rs.getObject("record_id"));
        log.setActionDate(rs.getTimestamp("action_date"));
        log.setDetails(rs.getString("details"));
        return log;
    }
}
