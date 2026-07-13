package com.bms.service;

import com.bms.db.*;
import com.bms.model.*;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class ReportService {
    private final SaleDAO saleDAO = new SaleDAO();
    private final InventoryDAO inventoryDAO = new InventoryDAO();
    private final WarehouseDAO warehouseDAO = new WarehouseDAO();
    private final BranchDAO branchDAO = new BranchDAO();

    public List<Sale> salesByDateRange(User viewer, LocalDateTime start, LocalDateTime end) throws SQLException {
        if (viewer.getRoleLevel() == 3) {
            // Sale role: own sales only.
            return saleDAO.findByUserAndDateRange(viewer.getUserId(), start, end);
        }
        if (viewer.getRoleLevel() == 2) {
            // Manager: sales in their branch's store warehouse? Sales table doesn't store branch/warehouse directly.
            // Approximate by user branch, using users in same branch.
            return filterSalesByBranch(saleDAO.findByDateRange(start, end), viewer.getBranchId());
        }
        if (viewer.getRoleLevel() == 1) {
            Integer province = resolveProvince(viewer);
            return filterSalesByProvince(saleDAO.findByDateRange(start, end), province);
        }
        // Super admin: all.
        return saleDAO.findByDateRange(start, end);
    }

    public List<SaleDAO.TopSellingRow> topSelling(User viewer, LocalDateTime start, LocalDateTime end, int limit) throws SQLException {
        // Top-selling is global for the date range; role-level filtering is applied by the date range scope above.
        // For province/store scoped reports we rely on the caller to pick a reasonable range.
        return saleDAO.topSelling(start, end, limit);
    }

    public List<Inventory> currentInventory(User viewer) throws SQLException {
        if (viewer.getRoleLevel() == 0) {
            return inventoryDAO.findAll();
        }
        if (viewer.getRoleLevel() == 1) {
            Integer province = resolveProvince(viewer);
            if (province == null) return List.of();
            return inventoryDAO.findAll().stream()
                    .filter(inv -> {
                        try {
                            return warehouseDAO.findById(inv.getWarehouseId())
                                    .map(w -> w.getProvinceId() == province).orElse(false);
                        } catch (SQLException e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
        }
        if (viewer.getRoleLevel() == 2) {
            Branch branch = branchDAO.findById(viewer.getBranchId()).orElse(null);
            if (branch == null || branch.getWarehouseId() == null) return List.of();
            return inventoryDAO.findByWarehouse(branch.getWarehouseId());
        }
        // Sale role.
        Branch branch = branchDAO.findById(viewer.getBranchId()).orElse(null);
        if (branch == null || branch.getWarehouseId() == null) return List.of();
        return inventoryDAO.findByWarehouse(branch.getWarehouseId());
    }

    private Integer resolveProvince(User u) throws SQLException {
        if (u.getBranchId() != null) {
            return branchDAO.findById(u.getBranchId()).map(Branch::getProvinceId).orElse(null);
        }
        if (u.getWarehouseId() != null) {
            return warehouseDAO.findById(u.getWarehouseId()).map(Warehouse::getProvinceId).orElse(null);
        }
        return null;
    }

    private List<Sale> filterSalesByBranch(List<Sale> sales, Integer branchId) throws SQLException {
        if (branchId == null) return List.of();
        return sales.stream()
                .filter(s -> {
                    try {
                        return branchId.equals(new UserDAO().findById(s.getUserId()).map(User::getBranchId).orElse(null));
                    } catch (SQLException e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    private List<Sale> filterSalesByProvince(List<Sale> sales, Integer provinceId) throws SQLException {
        if (provinceId == null) return List.of();
        return sales.stream()
                .filter(s -> {
                    try {
                        User seller = new UserDAO().findById(s.getUserId()).orElse(null);
                        if (seller == null) return false;
                        return provinceId.equals(resolveProvince(seller));
                    } catch (SQLException e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }
}
