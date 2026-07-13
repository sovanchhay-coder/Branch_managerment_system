package com.bms.service;

import com.bms.db.CategoryDAO;
import com.bms.model.Category;
import com.bms.model.User;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class CategoryService {
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final AuditService auditService = new AuditService();

    public List<Category> findAll() throws SQLException {
        return categoryDAO.findAll();
    }

    public Optional<Category> findById(int id) throws SQLException {
        return categoryDAO.findById(id);
    }

    public Category create(User actor, Category c) throws SQLException {
        Category created = categoryDAO.insert(c);
        auditService.log(actor.getUserId(), "CREATE", "categories", created.getCategoryId(),
                "Created category " + created.getCategoryName());
        return created;
    }

    public void update(User actor, Category c) throws SQLException {
        categoryDAO.update(c);
        auditService.log(actor.getUserId(), "UPDATE", "categories", c.getCategoryId(),
                "Updated category " + c.getCategoryName());
    }

    public void delete(User actor, int id) throws SQLException {
        categoryDAO.delete(id);
        auditService.log(actor.getUserId(), "DELETE", "categories", id, "Deleted category id " + id);
    }
}
