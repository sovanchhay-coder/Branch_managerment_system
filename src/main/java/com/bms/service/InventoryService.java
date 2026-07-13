package com.bms.service;

import com.bms.db.InventoryDAO;
import com.bms.model.Inventory;
import com.bms.model.User;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class InventoryService {
    private final InventoryDAO inventoryDAO = new InventoryDAO();
    private final AuditService auditService = new AuditService();

    public List<Inventory> findAll() throws SQLException {
        return inventoryDAO.findAll();
    }

    public List<Inventory> findByWarehouse(int warehouseId) throws SQLException {
        return inventoryDAO.findByWarehouse(warehouseId);
    }

    public Optional<Inventory> find(int productId, int warehouseId) throws SQLException {
        return inventoryDAO.findByProductAndWarehouse(productId, warehouseId);
    }

    public int adjust(User actor, int productId, int warehouseId, int delta, String reason) throws SQLException {
        int newQty = inventoryDAO.adjustQuantity(productId, warehouseId, delta);
        auditService.log(actor.getUserId(), delta >= 0 ? "STOCK_ADD" : "STOCK_REMOVE", "inventory", null,
                String.format("%s %d of product %d at warehouse %d -> qty %d (reason: %s)",
                        delta >= 0 ? "Added" : "Removed", Math.abs(delta), productId, warehouseId, newQty, reason));
        return newQty;
    }

    public void setQuantity(User actor, int productId, int warehouseId, int quantity) throws SQLException {
        inventoryDAO.setQuantity(productId, warehouseId, quantity);
        auditService.log(actor.getUserId(), "STOCK_SET", "inventory", null,
                "Set product " + productId + " at warehouse " + warehouseId + " to " + quantity);
    }

    public int getAvailable(int productId, int warehouseId) throws SQLException {
        return inventoryDAO.getQuantity(productId, warehouseId);
    }
}
