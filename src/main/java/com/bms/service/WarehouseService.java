package com.bms.service;

import com.bms.db.WarehouseDAO;
import com.bms.model.User;
import com.bms.model.Warehouse;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class WarehouseService {
    private final WarehouseDAO warehouseDAO = new WarehouseDAO();
    private final AuditService auditService = new AuditService();

    public List<Warehouse> findAll() throws SQLException {
        return warehouseDAO.findAll();
    }

    public Optional<Warehouse> findById(int id) throws SQLException {
        return warehouseDAO.findById(id);
    }

    public List<Warehouse> findByProvince(int provinceId) throws SQLException {
        return warehouseDAO.findByProvince(provinceId);
    }

    public List<Warehouse> findByType(int typeId) throws SQLException {
        return warehouseDAO.findByType(typeId);
    }

    public List<Warehouse> findByProvinceAndType(int provinceId, int typeId) throws SQLException {
        return warehouseDAO.findByProvinceAndType(provinceId, typeId);
    }

    public Warehouse create(User actor, Warehouse w) throws SQLException {
        Warehouse created = warehouseDAO.insert(w);
        auditService.log(actor.getUserId(), "CREATE", "warehouses", created.getWarehouseId(),
                "Created warehouse " + created.getName());
        return created;
    }

    public void update(User actor, Warehouse w) throws SQLException {
        warehouseDAO.update(w);
        auditService.log(actor.getUserId(), "UPDATE", "warehouses", w.getWarehouseId(),
                "Updated warehouse " + w.getName());
    }

    public void delete(User actor, int id) throws SQLException {
        warehouseDAO.delete(id);
        auditService.log(actor.getUserId(), "DELETE", "warehouses", id, "Deleted warehouse id " + id);
    }
}
