package com.bms.service;

import com.bms.db.BranchDAO;
import com.bms.db.RoleDAO;
import com.bms.db.UserDAO;
import com.bms.db.WarehouseDAO;
import com.bms.model.Role;
import com.bms.model.User;
import com.bms.util.PasswordHasher;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class UserService {
    private final UserDAO userDAO = new UserDAO();
    private final RoleDAO roleDAO = new RoleDAO();
    private final BranchDAO branchDAO = new BranchDAO();
    private final WarehouseDAO warehouseDAO = new WarehouseDAO();
    private final AuditService auditService = new AuditService();

    /**
     * Central, reusable scope check. Must be called before every user INSERT.
     *
     * @param creator       the user doing the creation
     * @param targetRoleId  role id of the new account
     * @param targetBranch  branch id selected for the new account (may be null)
     * @param targetWarehouse warehouse id selected for the new account (may be null)
     */
    public boolean canCreate(User creator, int targetRoleId, Integer targetBranch, Integer targetWarehouse) throws SQLException {
        Role targetRole = roleDAO.findById(targetRoleId).orElseThrow(() -> new SQLException("Role not found"));
        int creatorLevel = creator.getRoleLevel();
        int targetLevel = targetRole.getLevel();

        // You may only create roles strictly lower in the hierarchy.
        if (targetLevel <= creatorLevel) {
            return false;
        }

        // Super admin is unrestricted.
        if (creatorLevel == 0) {
            return true;
        }

        // Sup admin can create Manager / Sale only inside their province.
        if (creatorLevel == 1) {
            Integer creatorProvince = resolveCreatorProvince(creator);
            if (creatorProvince == null) return false;
            Integer targetProvince = resolveTargetProvince(targetBranch, targetWarehouse);
            return creatorProvince.equals(targetProvince);
        }

        // Manager can create Sale only inside their own branch.
        if (creatorLevel == 2) {
            if (targetLevel != 3) return false;
            return creator.getBranchId() != null && creator.getBranchId().equals(targetBranch);
        }

        // Sale cannot create accounts.
        return false;
    }

    public String canCreateReason(User creator, int targetRoleId, Integer targetBranch, Integer targetWarehouse) throws SQLException {
        if (canCreate(creator, targetRoleId, targetBranch, targetWarehouse)) {
            return null;
        }
        return "You are not allowed to create this account (role/scope mismatch).";
    }

    public User createUser(User creator, User newUser, String plaintextPassword) throws Exception {
        if (!canCreate(creator, newUser.getRoleId(), newUser.getBranchId(), newUser.getWarehouseId())) {
            throw new SecurityException("Account creation not permitted for this role/scope.");
        }
        if (newUser.getUsername() == null || newUser.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (plaintextPassword == null || plaintextPassword.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }
        userDAO.findByUsername(newUser.getUsername()).ifPresent(u -> {
            throw new IllegalArgumentException("Username already exists.");
        });

        newUser.setPasswordHash(PasswordHasher.hash(plaintextPassword));
        User created = userDAO.insert(newUser);
        auditService.log(creator.getUserId(), "CREATE", "users", created.getUserId(),
                "Created user " + created.getUsername() + " with role " + created.getRoleId());
        return created;
    }

    public List<User> findAll() throws SQLException {
        return userDAO.findAll();
    }

    public Optional<User> findById(int id) throws SQLException {
        return userDAO.findById(id);
    }

    public List<Role> findAllRoles() throws SQLException {
        return roleDAO.findAll();
    }

    public List<Role> getCreatableRoles(User creator) throws SQLException {
        return roleDAO.findAll().stream()
                .filter(r -> r.getLevel() > creator.getRoleLevel())
                .toList();
    }

    public void deleteUser(User actor, int userId) throws Exception {
        if (actor.getUserId() == userId) {
            throw new SecurityException("You cannot delete your own account.");
        }
        User target = userDAO.findById(userId).orElseThrow(() -> new SQLException("User not found"));
        if (target.getRoleLevel() <= actor.getRoleLevel()) {
            throw new SecurityException("You cannot delete a user with equal or higher role.");
        }
        userDAO.delete(userId);
        auditService.log(actor.getUserId(), "DELETE", "users", userId, "Deleted user " + target.getUsername());
    }

    private Integer resolveCreatorProvince(User creator) throws SQLException {
        if (creator.getBranchId() != null) {
            return branchDAO.findById(creator.getBranchId()).map(b -> b.getProvinceId()).orElse(null);
        }
        if (creator.getWarehouseId() != null) {
            return warehouseDAO.findById(creator.getWarehouseId()).map(w -> w.getProvinceId()).orElse(null);
        }
        return null;
    }

    private Integer resolveTargetProvince(Integer targetBranch, Integer targetWarehouse) throws SQLException {
        if (targetBranch != null) {
            return branchDAO.findById(targetBranch).map(b -> b.getProvinceId()).orElse(null);
        }
        if (targetWarehouse != null) {
            return warehouseDAO.findById(targetWarehouse).map(w -> w.getProvinceId()).orElse(null);
        }
        return null;
    }
}
