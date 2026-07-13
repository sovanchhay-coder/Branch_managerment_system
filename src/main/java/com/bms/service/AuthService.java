package com.bms.service;

import com.bms.db.UserDAO;
import com.bms.model.User;
import com.bms.util.PasswordHasher;

import java.sql.SQLException;
import java.util.Optional;

public class AuthService {
    private final UserDAO userDAO = new UserDAO();

    public Optional<User> authenticate(String username, String password) throws SQLException {
        Optional<User> opt = userDAO.findByUsername(username);
        if (opt.isPresent() && PasswordHasher.verify(password, opt.get().getPasswordHash())) {
            return opt;
        }
        return Optional.empty();
    }
}
