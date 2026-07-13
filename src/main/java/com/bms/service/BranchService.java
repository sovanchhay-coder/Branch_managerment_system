package com.bms.service;

import com.bms.db.BranchDAO;
import com.bms.model.Branch;
import com.bms.model.User;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class BranchService {
    private final BranchDAO branchDAO = new BranchDAO();
    private final AuditService auditService = new AuditService();

    public List<Branch> findAll() throws SQLException {
        return branchDAO.findAll();
    }

    public Optional<Branch> findById(int id) throws SQLException {
        return branchDAO.findById(id);
    }

    public List<Branch> findByProvince(int provinceId) throws SQLException {
        return branchDAO.findByProvince(provinceId);
    }

    public Branch create(User actor, Branch b) throws SQLException {
        Branch created = branchDAO.insert(b);
        auditService.log(actor.getUserId(), "CREATE", "branches", created.getBranchId(),
                "Created branch " + created.getBranchName());
        return created;
    }

    public void update(User actor, Branch b) throws SQLException {
        branchDAO.update(b);
        auditService.log(actor.getUserId(), "UPDATE", "branches", b.getBranchId(),
                "Updated branch " + b.getBranchName());
    }

    public void delete(User actor, int id) throws SQLException {
        branchDAO.delete(id);
        auditService.log(actor.getUserId(), "DELETE", "branches", id, "Deleted branch id " + id);
    }
}
