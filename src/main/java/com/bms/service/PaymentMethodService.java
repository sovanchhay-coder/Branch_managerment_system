package com.bms.service;

import com.bms.db.PaymentMethodDAO;
import com.bms.model.PaymentMethod;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class PaymentMethodService {
    private final PaymentMethodDAO paymentMethodDAO = new PaymentMethodDAO();

    public List<PaymentMethod> findAll() throws SQLException {
        return paymentMethodDAO.findAll();
    }

    public Optional<PaymentMethod> findById(int id) throws SQLException {
        return paymentMethodDAO.findById(id);
    }
}
