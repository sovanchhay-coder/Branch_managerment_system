package com.bms.service;

import com.bms.db.WarehouseTypeDAO;
import com.bms.model.WarehouseType;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class WarehouseTypeService {
    private final WarehouseTypeDAO warehouseTypeDAO = new WarehouseTypeDAO();

    public List<WarehouseType> findAll() throws SQLException {
        return warehouseTypeDAO.findAll();
    }

    public Optional<WarehouseType> findById(int id) throws SQLException {
        return warehouseTypeDAO.findById(id);
    }
}
