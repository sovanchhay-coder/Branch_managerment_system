package com.bms.db;

import com.bms.model.PaymentMethod;
import com.bms.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PaymentMethodDAO {

    public List<PaymentMethod> findAll() throws SQLException {
        String sql = "SELECT payment_method_id, method_name FROM payment_methods ORDER BY method_name";
        List<PaymentMethod> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public Optional<PaymentMethod> findById(int id) throws SQLException {
        String sql = "SELECT payment_method_id, method_name FROM payment_methods WHERE payment_method_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    public PaymentMethod insert(PaymentMethod pm) throws SQLException {
        String sql = "INSERT INTO payment_methods (method_name) VALUES (?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, pm.getMethodName());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    pm.setPaymentMethodId(rs.getInt(1));
                }
            }
        }
        return pm;
    }

    private PaymentMethod map(ResultSet rs) throws SQLException {
        return new PaymentMethod(rs.getInt("payment_method_id"), rs.getString("method_name"));
    }
}
