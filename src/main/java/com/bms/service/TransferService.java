package com.bms.service;

import com.bms.db.InventoryDAO;
import com.bms.db.TransferDAO;
import com.bms.model.Transfer;
import com.bms.model.User;
import com.bms.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class TransferService {
    private final TransferDAO transferDAO = new TransferDAO();
    private final InventoryDAO inventoryDAO = new InventoryDAO();
    private final AuditService auditService = new AuditService();

    public List<Transfer> findAll() throws SQLException {
        return transferDAO.findAll();
    }

    public List<Transfer> findPending() throws SQLException {
        return transferDAO.findPending();
    }

    public Optional<Transfer> findById(int id) throws SQLException {
        return transferDAO.findById(id);
    }

    public Transfer create(User actor, Transfer t) throws SQLException {
        t.setApprovedBy(null);
        Transfer created = transferDAO.insert(t);
        auditService.log(actor.getUserId(), "CREATE", "transfers", created.getTransferId(),
                "Created transfer of product " + created.getProductId() +
                " qty " + created.getQuantity() + " from " + created.getFromWarehouseId() +
                " to " + created.getToWarehouseId());
        return created;
    }

    public void approve(User actor, int transferId) throws SQLException {
        Transfer t = transferDAO.findById(transferId)
                .orElseThrow(() -> new SQLException("Transfer not found"));
        if (t.isApproved()) {
            throw new IllegalStateException("Transfer already approved.");
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Deduct from source and add to destination atomically.
                inventoryDAO.adjustQuantity(conn, t.getProductId(), t.getFromWarehouseId(), -t.getQuantity());
                inventoryDAO.adjustQuantity(conn, t.getProductId(), t.getToWarehouseId(), t.getQuantity());
                transferDAO.approve(transferId, actor.getUserId());
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
        auditService.log(actor.getUserId(), "APPROVE", "transfers", transferId,
                "Approved transfer id " + transferId);
    }
}
