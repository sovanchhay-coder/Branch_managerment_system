package com.bms.service;

import com.bms.db.ProvinceDAO;
import com.bms.model.Province;
import com.bms.model.User;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ProvinceService {
    private final ProvinceDAO provinceDAO = new ProvinceDAO();
    private final AuditService auditService = new AuditService();

    public List<Province> findAll() throws SQLException {
        return provinceDAO.findAll();
    }

    public Optional<Province> findById(int id) throws SQLException {
        return provinceDAO.findById(id);
    }

    public Province create(User actor, Province p) throws SQLException {
        Province created = provinceDAO.insert(p);
        auditService.log(actor.getUserId(), "CREATE", "provinces", created.getProvinceId(),
                "Created province " + created.getProvinceName());
        return created;
    }

    public void update(User actor, Province p) throws SQLException {
        provinceDAO.update(p);
        auditService.log(actor.getUserId(), "UPDATE", "provinces", p.getProvinceId(),
                "Updated province " + p.getProvinceName());
    }

    public void delete(User actor, int id) throws SQLException {
        provinceDAO.delete(id);
        auditService.log(actor.getUserId(), "DELETE", "provinces", id, "Deleted province id " + id);
    }
}
