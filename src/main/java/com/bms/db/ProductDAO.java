package com.bms.db;

import com.bms.model.Product;
import com.bms.util.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductDAO {

    public List<Product> findAll() throws SQLException {
        String sql = baseSql() + " ORDER BY p.product_name";
        return query(sql, null);
    }

    public Optional<Product> findById(int id) throws SQLException {
        String sql = baseSql() + " WHERE p.product_id = ?";
        List<Product> list = query(sql, ps -> ps.setInt(1, id));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<Product> findByNameLike(String term) throws SQLException {
        String sql = baseSql() + " WHERE p.product_name LIKE ? ORDER BY p.product_name";
        return query(sql, ps -> ps.setString(1, "%" + term + "%"));
    }

    public List<Product> findByCategory(int categoryId) throws SQLException {
        String sql = baseSql() + " WHERE p.category_id = ? ORDER BY p.product_name";
        return query(sql, ps -> ps.setInt(1, categoryId));
    }

    public Product insert(Product p) throws SQLException {
        String sql = "INSERT INTO products (category_id, product_name, price, description, image_path) VALUES (?,?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getCategoryId());
            ps.setString(2, p.getProductName());
            ps.setBigDecimal(3, p.getPrice());
            ps.setString(4, p.getDescription());
            ps.setString(5, p.getImagePath());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    p.setProductId(rs.getInt(1));
                }
            }
        }
        return p;
    }

    public void update(Product p) throws SQLException {
        String sql = "UPDATE products SET category_id=?, product_name=?, price=?, description=?, image_path=? WHERE product_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, p.getCategoryId());
            ps.setString(2, p.getProductName());
            ps.setBigDecimal(3, p.getPrice());
            ps.setString(4, p.getDescription());
            ps.setString(5, p.getImagePath());
            ps.setInt(6, p.getProductId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM products WHERE product_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private String baseSql() {
        return "SELECT p.product_id, p.category_id, c.category_name, p.product_name, p.price, p.description, p.image_path " +
               "FROM products p JOIN categories c ON p.category_id = c.category_id";
    }

    @FunctionalInterface
    private interface ParamSetter {
        void set(PreparedStatement ps) throws SQLException;
    }

    private List<Product> query(String sql, ParamSetter setter) throws SQLException {
        List<Product> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (setter != null) setter.set(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    private Product map(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setProductId(rs.getInt("product_id"));
        p.setCategoryId(rs.getInt("category_id"));
        p.setCategoryName(rs.getString("category_name"));
        p.setProductName(rs.getString("product_name"));
        p.setPrice(rs.getBigDecimal("price"));
        p.setDescription(rs.getString("description"));
        p.setImagePath(rs.getString("image_path"));
        return p;
    }
}
