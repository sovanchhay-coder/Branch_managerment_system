package com.bms.service;

import com.bms.db.AuditLogDAO;
import com.bms.model.AuditLog;

import java.sql.SQLException;

public class AuditService {
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    public void log(Integer userId, String action, String tableName, Integer recordId, String details) {
        try {
            AuditLog log = new AuditLog();
            log.setUserId(userId);
            log.setAction(action);
            log.setTableName(tableName);
            log.setRecordId(recordId);
            log.setDetails(details);
            auditLogDAO.insert(log);
        } catch (SQLException e) {
            // Audit failures must not break the business transaction.
            System.err.println("Audit log failed: " + e.getMessage());
        }
    }
}
