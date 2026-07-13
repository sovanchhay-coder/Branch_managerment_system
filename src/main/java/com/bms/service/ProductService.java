package com.bms.service;

import com.bms.db.ProductDAO;
import com.bms.model.Product;
import com.bms.model.User;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ProductService {
    private final ProductDAO productDAO = new ProductDAO();
    private final AuditService auditService = new AuditService();

    public List<Product> findAll() throws SQLException {
        return productDAO.findAll();
    }

    public Optional<Product> findById(int id) throws SQLException {
        return productDAO.findById(id);
    }

    public List<Product> searchByName(String term) throws SQLException {
        return productDAO.findByNameLike(term);
    }

    public Product create(User actor, Product p) throws SQLException {
        Product created = productDAO.insert(p);
        auditService.log(actor.getUserId(), "CREATE", "products", created.getProductId(),
                "Created product " + created.getProductName());
        return created;
    }

    public void update(User actor, Product p) throws SQLException {
        productDAO.update(p);
        auditService.log(actor.getUserId(), "UPDATE", "products", p.getProductId(),
                "Updated product " + p.getProductName());
    }

    public void delete(User actor, int id) throws SQLException {
        productDAO.delete(id);
        auditService.log(actor.getUserId(), "DELETE", "products", id, "Deleted product id " + id);
    }
}
