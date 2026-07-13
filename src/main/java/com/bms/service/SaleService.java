package com.bms.service;

import com.bms.db.BranchDAO;
import com.bms.db.InventoryDAO;
import com.bms.db.SaleDAO;
import com.bms.model.Branch;
import com.bms.model.Sale;
import com.bms.model.SaleItem;
import com.bms.model.User;
import com.bms.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class SaleService {
    private final SaleDAO saleDAO = new SaleDAO();
    private final InventoryDAO inventoryDAO = new InventoryDAO();
    private final BranchDAO branchDAO = new BranchDAO();
    private final AuditService auditService = new AuditService();

    public Sale processSale(User seller, int paymentMethodId, List<SaleItem> cart) throws SQLException {
        if (cart == null || cart.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty.");
        }
        if (seller.getBranchId() == null) {
            throw new IllegalStateException("Seller is not assigned to a branch.");
        }
        Branch branch = branchDAO.findById(seller.getBranchId())
                .orElseThrow(() -> new SQLException("Branch not found"));
        if (branch.getWarehouseId() == null) {
            throw new IllegalStateException("Branch has no store warehouse.");
        }
        int storeWarehouseId = branch.getWarehouseId();

        BigDecimal total = cart.stream()
                .map(SaleItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Verify stock and decrement.
                for (SaleItem item : cart) {
                    int available = inventoryDAO.getQuantity(item.getProductId(), storeWarehouseId);
                    if (item.getQuantity() > available) {
                        throw new SQLException("Insufficient stock for product " + item.getProductName() +
                                " (available: " + available + ", requested: " + item.getQuantity() + ")");
                    }
                }

                Sale sale = new Sale();
                sale.setUserId(seller.getUserId());
                sale.setPaymentMethodId(paymentMethodId);
                sale.setTotalAmount(total);
                saleDAO.insert(sale);

                for (SaleItem item : cart) {
                    item.setSaleId(sale.getSaleId());
                    saleDAO.insertItem(item);
                    inventoryDAO.adjustQuantity(conn, item.getProductId(), storeWarehouseId, -item.getQuantity());
                    auditService.log(seller.getUserId(), "SALE_DECREMENT", "inventory", null,
                            "Sold " + item.getQuantity() + " of product " + item.getProductId() +
                            " from warehouse " + storeWarehouseId);
                }

                conn.commit();
                auditService.log(seller.getUserId(), "CREATE", "sales", sale.getSaleId(),
                        "Sale total " + total + " items " + cart.size());
                return sale;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public List<Sale> findByUser(int userId) throws SQLException {
        return saleDAO.findByUser(userId);
    }

    public List<Sale> findAll() throws SQLException {
        return saleDAO.findAll();
    }

    public Optional<Sale> findById(int id) throws SQLException {
        return saleDAO.findById(id);
    }
}
