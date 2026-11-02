-- StockFlow schema — SAFE to run more than once: it never drops or deletes anything.
-- (Existing tables are left untouched. To wipe everything on purpose, see 99_reset_database_DESTRUCTIVE.sql)
CREATE DATABASE IF NOT EXISTS stockflow_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE stockflow_db;

CREATE TABLE IF NOT EXISTS users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    role          ENUM('ADMIN','MANAGER','STAFF') NOT NULL DEFAULT 'STAFF',
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS categories (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(80)  NOT NULL UNIQUE,
    description VARCHAR(255),
    active      BOOLEAN      NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS suppliers (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(120) NOT NULL,
    phone      VARCHAR(20)  NOT NULL,
    email      VARCHAR(100),
    address    VARCHAR(255),
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_suppliers_name (name),
    INDEX idx_suppliers_phone (phone)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS customers (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(120) NOT NULL,
    phone      VARCHAR(20)  NOT NULL,
    email      VARCHAR(100),
    address    VARCHAR(255),
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_customers_name (name),
    INDEX idx_customers_phone (phone)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS products (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    sku           VARCHAR(40)   NOT NULL UNIQUE,
    name          VARCHAR(150)  NOT NULL,
    category_id   INT           NOT NULL,
    supplier_id   INT           NULL,
    brand         VARCHAR(60),
    cost_price    DECIMAL(12,2) NOT NULL,
    selling_price DECIMAL(12,2) NOT NULL,
    quantity      INT           NOT NULL DEFAULT 0,
    reorder_level INT           NOT NULL DEFAULT 5,
    active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_prod_cost  CHECK (cost_price >= 0),
    CONSTRAINT chk_prod_sell  CHECK (selling_price >= 0),
    CONSTRAINT chk_prod_qty   CHECK (quantity >= 0),
    CONSTRAINT chk_prod_reord CHECK (reorder_level >= 0),
    CONSTRAINT fk_prod_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT fk_prod_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE SET NULL,
    INDEX idx_products_name (name),
    INDEX idx_products_brand (brand)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS purchases (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    supplier_id   INT           NOT NULL,
    user_id       INT           NOT NULL,
    total_amount  DECIMAL(14,2) NOT NULL,
    purchase_date TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note          VARCHAR(255),
    CONSTRAINT chk_pur_total CHECK (total_amount >= 0),
    CONSTRAINT fk_pur_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    CONSTRAINT fk_pur_user     FOREIGN KEY (user_id)     REFERENCES users(id),
    INDEX idx_purchases_date (purchase_date)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS purchase_items (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    purchase_id INT           NOT NULL,
    product_id  INT           NOT NULL,
    quantity    INT           NOT NULL,
    unit_cost   DECIMAL(12,2) NOT NULL,
    subtotal    DECIMAL(14,2) NOT NULL,
    CONSTRAINT chk_pi_qty  CHECK (quantity > 0),
    CONSTRAINT chk_pi_cost CHECK (unit_cost >= 0),
    CONSTRAINT fk_pi_purchase FOREIGN KEY (purchase_id) REFERENCES purchases(id) ON DELETE CASCADE,
    CONSTRAINT fk_pi_product  FOREIGN KEY (product_id)  REFERENCES products(id),
    INDEX idx_pi_purchase (purchase_id),
    INDEX idx_pi_product (product_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sales (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    invoice_no   VARCHAR(30)   NOT NULL UNIQUE,
    customer_id  INT           NULL,
    user_id      INT           NOT NULL,
    total_amount DECIMAL(14,2) NOT NULL,
    sale_date    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_sale_total CHECK (total_amount >= 0),
    CONSTRAINT fk_sale_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL,
    CONSTRAINT fk_sale_user     FOREIGN KEY (user_id)     REFERENCES users(id),
    INDEX idx_sales_date (sale_date)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sale_items (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    sale_id    INT           NOT NULL,
    product_id INT           NOT NULL,
    quantity   INT           NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    unit_cost  DECIMAL(12,2) NOT NULL,
    subtotal   DECIMAL(14,2) NOT NULL,
    CONSTRAINT chk_si_qty   CHECK (quantity > 0),
    CONSTRAINT chk_si_price CHECK (unit_price >= 0),
    CONSTRAINT fk_si_sale    FOREIGN KEY (sale_id)    REFERENCES sales(id) ON DELETE CASCADE,
    CONSTRAINT fk_si_product FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_si_sale (sale_id),
    INDEX idx_si_product (product_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS activity_logs (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT          NULL,
    action     VARCHAR(60)  NOT NULL,
    details    VARCHAR(500),
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_logs_created (created_at)
) ENGINE=InnoDB;
