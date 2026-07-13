-- Branch Management System - full schema + seed data
-- Run this script against a MySQL server to create the database and sample users.

CREATE DATABASE IF NOT EXISTS branch_management
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE branch_management;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS sale_items;
DROP TABLE IF EXISTS sales;
DROP TABLE IF EXISTS transfers;
DROP TABLE IF EXISTS inventory;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS branches;
DROP TABLE IF EXISTS warehouses;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS payment_methods;
DROP TABLE IF EXISTS warehouse_types;
DROP TABLE IF EXISTS provinces;
DROP TABLE IF EXISTS role_permissions;
DROP TABLE IF EXISTS permissions;
DROP TABLE IF EXISTS roles;

SET FOREIGN_KEY_CHECKS = 1;

-- Roles (self-referencing hierarchy)
CREATE TABLE roles (
    role_id        INT AUTO_INCREMENT PRIMARY KEY,
    role_name      VARCHAR(50) NOT NULL UNIQUE,
    parent_role_id INT DEFAULT NULL,
    level          INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_roles_parent FOREIGN KEY (parent_role_id) REFERENCES roles (role_id)
);

CREATE TABLE permissions (
    permission_id   INT AUTO_INCREMENT PRIMARY KEY,
    permission_name VARCHAR(100) NOT NULL UNIQUE,
    description     VARCHAR(255) DEFAULT NULL
);

CREATE TABLE role_permissions (
    role_id       INT NOT NULL,
    permission_id INT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES roles (role_id) ON DELETE CASCADE,
    CONSTRAINT fk_rp_perm FOREIGN KEY (permission_id) REFERENCES permissions (permission_id) ON DELETE CASCADE
);

CREATE TABLE provinces (
    province_id   INT AUTO_INCREMENT PRIMARY KEY,
    province_name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE warehouse_types (
    type_id   INT AUTO_INCREMENT PRIMARY KEY,
    type_name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE payment_methods (
    payment_method_id INT AUTO_INCREMENT PRIMARY KEY,
    method_name       VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE categories (
    category_id   INT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE warehouses (
    warehouse_id        INT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(100) NOT NULL,
    type_id             INT NOT NULL,
    location            VARCHAR(255) DEFAULT NULL,
    province_id         INT NOT NULL,
    parent_warehouse_id INT DEFAULT NULL,
    CONSTRAINT fk_wh_type FOREIGN KEY (type_id) REFERENCES warehouse_types (type_id),
    CONSTRAINT fk_wh_province FOREIGN KEY (province_id) REFERENCES provinces (province_id),
    CONSTRAINT fk_wh_parent FOREIGN KEY (parent_warehouse_id) REFERENCES warehouses (warehouse_id)
);

CREATE TABLE branches (
    branch_id   INT AUTO_INCREMENT PRIMARY KEY,
    branch_name VARCHAR(100) NOT NULL,
    warehouse_id INT DEFAULT NULL,
    province_id  INT DEFAULT NULL,
    address      VARCHAR(255) DEFAULT NULL,
    CONSTRAINT fk_branch_wh FOREIGN KEY (warehouse_id) REFERENCES warehouses (warehouse_id),
    CONSTRAINT fk_branch_province FOREIGN KEY (province_id) REFERENCES provinces (province_id)
);

CREATE TABLE users (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role_id       INT NOT NULL,
    branch_id     INT DEFAULT NULL,
    warehouse_id  INT DEFAULT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES roles (role_id),
    CONSTRAINT fk_user_branch FOREIGN KEY (branch_id) REFERENCES branches (branch_id),
    CONSTRAINT fk_user_wh FOREIGN KEY (warehouse_id) REFERENCES warehouses (warehouse_id)
);

CREATE TABLE products (
    product_id   INT AUTO_INCREMENT PRIMARY KEY,
    category_id  INT NOT NULL,
    product_name VARCHAR(150) NOT NULL,
    price        DECIMAL(10,2) NOT NULL,
    description  TEXT,
    image_path   VARCHAR(255) DEFAULT NULL,
    CONSTRAINT fk_product_cat FOREIGN KEY (category_id) REFERENCES categories (category_id)
);

CREATE TABLE inventory (
    inventory_id  INT AUTO_INCREMENT PRIMARY KEY,
    product_id    INT NOT NULL,
    warehouse_id  INT NOT NULL,
    quantity      INT NOT NULL DEFAULT 0,
    last_updated  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_inv_product_wh (product_id, warehouse_id),
    CONSTRAINT fk_inv_product FOREIGN KEY (product_id) REFERENCES products (product_id),
    CONSTRAINT fk_inv_wh FOREIGN KEY (warehouse_id) REFERENCES warehouses (warehouse_id)
);

CREATE TABLE transfers (
    transfer_id       INT AUTO_INCREMENT PRIMARY KEY,
    from_warehouse_id INT NOT NULL,
    to_warehouse_id   INT NOT NULL,
    product_id        INT NOT NULL,
    quantity          INT NOT NULL,
    transfer_date     DATETIME DEFAULT CURRENT_TIMESTAMP,
    approved_by       INT DEFAULT NULL,
    CONSTRAINT fk_transfer_from FOREIGN KEY (from_warehouse_id) REFERENCES warehouses (warehouse_id),
    CONSTRAINT fk_transfer_to FOREIGN KEY (to_warehouse_id) REFERENCES warehouses (warehouse_id),
    CONSTRAINT fk_transfer_product FOREIGN KEY (product_id) REFERENCES products (product_id),
    CONSTRAINT fk_transfer_approver FOREIGN KEY (approved_by) REFERENCES users (user_id)
);

CREATE TABLE sales (
    sale_id           INT AUTO_INCREMENT PRIMARY KEY,
    user_id           INT NOT NULL,
    payment_method_id INT NOT NULL,
    sale_date         DATETIME DEFAULT CURRENT_TIMESTAMP,
    total_amount      DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_sale_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_sale_payment FOREIGN KEY (payment_method_id) REFERENCES payment_methods (payment_method_id)
);

CREATE TABLE sale_items (
    item_id    INT AUTO_INCREMENT PRIMARY KEY,
    sale_id    INT NOT NULL,
    product_id INT NOT NULL,
    quantity   INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    subtotal   DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_si_sale FOREIGN KEY (sale_id) REFERENCES sales (sale_id) ON DELETE CASCADE,
    CONSTRAINT fk_si_product FOREIGN KEY (product_id) REFERENCES products (product_id)
);

CREATE TABLE audit_logs (
    log_id      INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT DEFAULT NULL,
    action      VARCHAR(100) NOT NULL,
    table_name  VARCHAR(100) DEFAULT NULL,
    record_id   INT DEFAULT NULL,
    action_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    details     TEXT,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users (user_id)
);

-- Seed static lookup data
INSERT INTO roles (role_id, role_name, parent_role_id, level) VALUES
(1, 'Super admin', NULL, 0),
(2, 'Sup admin',    1,    1),
(3, 'Manager',      2,    2),
(4, 'Sale',         3,    3);

INSERT INTO permissions (permission_id, permission_name, description) VALUES
(1, 'manage_main_warehouse',     'Full control over the main warehouse'),
(2, 'manage_province_warehouse', 'Control warehouse stock at a province level'),
(3, 'control_store_warehouse',   'Control warehouse/inventory at a specific store'),
(4, 'sell_product',              'Ability to sell products / create sales'),
(5, 'view_reports',              'Ability to view sales and inventory reports');

INSERT INTO role_permissions (role_id, permission_id) VALUES
(1,1),(1,2),(2,2),(1,3),(2,3),(3,3),(1,4),(4,4),(1,5),(2,5),(3,5),(4,5);

INSERT INTO warehouse_types (type_id, type_name) VALUES
(1, 'Main'),
(2, 'Province'),
(3, 'Store');

INSERT INTO payment_methods (payment_method_id, method_name) VALUES
(1, 'Cash'),
(2, 'Credit Card'),
(3, 'Bank Transfer');

-- Seed sample province / warehouse / branch / category / product / inventory
INSERT INTO provinces (province_id, province_name) VALUES (1, 'Phnom Penh');

INSERT INTO warehouses (warehouse_id, name, type_id, location, province_id, parent_warehouse_id) VALUES
(1, 'Main Warehouse',      1, 'National Hub',         1, NULL),
(2, 'PP Province Warehouse', 2, 'Phnom Penh Province', 1, 1),
(3, 'PP Store Warehouse',    3, 'Phnom Penh Store',    1, 2);

INSERT INTO branches (branch_id, branch_name, warehouse_id, province_id, address) VALUES
(1, 'PP Downtown Branch', 3, 1, '123 Monivong Blvd');

INSERT INTO categories (category_id, category_name) VALUES
(1, 'Electronics');

INSERT INTO products (product_id, category_id, product_name, price, description) VALUES
(1, 1, 'Laptop', 1200.00, 'Business laptop'),
(2, 1, 'Mouse',    25.00, 'Wireless mouse');

INSERT INTO inventory (product_id, warehouse_id, quantity) VALUES
(1, 1, 100),
(2, 1, 200),
(1, 2,  50),
(2, 2,  80),
(1, 3,  20),
(2, 3,  50);

-- Seed sample users (all passwords are listed in the README)
-- superadmin / password
-- supadmin   / suppass
-- manager    / managerpass
-- sale       / salepass
INSERT INTO users (username, password_hash, role_id, branch_id, warehouse_id) VALUES
('superadmin', '$2a$12$1HezmXiUZ/nARry2Ehd7Xeib6.uze/mqGxrbKzX/eT1nXgmAQdU8e', 1, NULL, NULL),
('supadmin',   '$2a$12$SuM9b5ONr9H.o7Dq/ZumRO2Yc3ABdVbV3vmNe56G5my57ZO4TE8gy', 2, NULL, 2),
('manager',    '$2a$12$ZMg.8WtTkc4K3TpYDsRE2uI5B1f/I.4EjV3mfXTxSforK7xleNgYC', 3, 1,    NULL),
('sale',       '$2a$12$Dx.JO.517p3fnMrmkOZiKeFRlo6ChudGnYfKy343wySXbXA/thaE.', 4, 1,    NULL);
