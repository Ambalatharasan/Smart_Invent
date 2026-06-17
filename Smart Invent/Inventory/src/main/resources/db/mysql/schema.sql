-- Smart Invent MySQL schema
-- Run this file before seed-data.sql when setting up MySQL manually.

CREATE DATABASE IF NOT EXISTS stockwise_inventory
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE stockwise_inventory;

CREATE TABLE IF NOT EXISTS user_accounts (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL,
  email VARCHAR(160) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(20) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  email_verified BOOLEAN NOT NULL DEFAULT FALSE,
  verified_at TIMESTAMP(6) NULL,
  last_login_at TIMESTAMP(6) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_accounts_email (email),
  CONSTRAINT chk_user_accounts_role CHECK (role IN ('ADMIN', 'MANAGER', 'VIEWER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE user_accounts ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE AFTER active',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'user_accounts'
    AND COLUMN_NAME = 'email_verified'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE user_accounts ADD COLUMN verified_at TIMESTAMP(6) NULL AFTER email_verified',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'user_accounts'
    AND COLUMN_NAME = 'verified_at'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE user_accounts ADD COLUMN last_login_at TIMESTAMP(6) NULL AFTER verified_at',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'user_accounts'
    AND COLUMN_NAME = 'last_login_at'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE user_accounts ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) AFTER created_at',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'user_accounts'
    AND COLUMN_NAME = 'updated_at'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS user_profiles (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_account_id BIGINT NOT NULL,
  display_name VARCHAR(120) NOT NULL,
  phone VARCHAR(40),
  department VARCHAR(80),
  job_title VARCHAR(120),
  location VARCHAR(120),
  bio VARCHAR(500),
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_profiles_user (user_account_id),
  KEY idx_user_profiles_department (department),
  CONSTRAINT fk_user_profiles_user
    FOREIGN KEY (user_account_id) REFERENCES user_accounts (id)
    ON UPDATE CASCADE
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS email_verification_codes (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_account_id BIGINT NOT NULL,
  email VARCHAR(160) NOT NULL,
  code_hash VARCHAR(255) NOT NULL,
  purpose VARCHAR(40) NOT NULL,
  expires_at TIMESTAMP(6) NOT NULL,
  consumed_at TIMESTAMP(6) NULL,
  attempts INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_email_verification_email_purpose (email, purpose, consumed_at, created_at),
  KEY idx_email_verification_user_purpose (user_account_id, purpose, consumed_at),
  KEY idx_email_verification_expires_at (expires_at),
  CONSTRAINT fk_email_verification_user
    FOREIGN KEY (user_account_id) REFERENCES user_accounts (id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  CONSTRAINT chk_email_verification_purpose CHECK (purpose IN ('REGISTRATION')),
  CONSTRAINT chk_email_verification_attempts CHECK (attempts >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS login_events (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_account_id BIGINT,
  email VARCHAR(160) NOT NULL,
  successful BOOLEAN NOT NULL,
  failure_reason VARCHAR(180),
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_login_events_created_at (created_at),
  KEY idx_login_events_email_created_at (email, created_at),
  KEY idx_login_events_user_created_at (user_account_id, created_at),
  CONSTRAINT fk_login_events_user
    FOREIGN KEY (user_account_id) REFERENCES user_accounts (id)
    ON UPDATE CASCADE
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS inventory_items (
  id BIGINT NOT NULL AUTO_INCREMENT,
  sku VARCHAR(80) NOT NULL,
  name VARCHAR(160) NOT NULL,
  category VARCHAR(80) NOT NULL,
  supplier VARCHAR(120) NOT NULL,
  quantity INT NOT NULL,
  reorder_point INT NOT NULL,
  unit_cost DECIMAL(12, 2) NOT NULL,
  retail_price DECIMAL(12, 2) NOT NULL,
  lead_time_days INT NOT NULL,
  location VARCHAR(80) NOT NULL,
  notes VARCHAR(500),
  last_restock_date DATE,
  last_restock_quantity INT,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_inventory_items_sku (sku),
  KEY idx_inventory_items_category (category),
  KEY idx_inventory_items_name (name),
  KEY idx_inventory_items_stock (quantity, reorder_point),
  CONSTRAINT chk_inventory_items_quantity CHECK (quantity >= 0),
  CONSTRAINT chk_inventory_items_reorder_point CHECK (reorder_point >= 0),
  CONSTRAINT chk_inventory_items_unit_cost CHECK (unit_cost >= 0),
  CONSTRAINT chk_inventory_items_retail_price CHECK (retail_price >= 0),
  CONSTRAINT chk_inventory_items_retail_ge_cost CHECK (retail_price >= unit_cost),
  CONSTRAINT chk_inventory_items_lead_time CHECK (lead_time_days >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS store_managers (
  id BIGINT NOT NULL AUTO_INCREMENT,
  full_name VARCHAR(120) NOT NULL,
  title VARCHAR(120) NOT NULL,
  department VARCHAR(80) NOT NULL,
  email VARCHAR(160) NOT NULL,
  phone VARCHAR(40) NOT NULL,
  shift_name VARCHAR(40) NOT NULL,
  status VARCHAR(40) NOT NULL,
  responsibilities VARCHAR(500),
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_store_managers_email (email),
  KEY idx_store_managers_department (department)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS restock_orders (
  id BIGINT NOT NULL AUTO_INCREMENT,
  inventory_item_id BIGINT NOT NULL,
  requested_quantity INT NOT NULL,
  estimated_cost DECIMAL(12, 2) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
  requested_by VARCHAR(160) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  received_at TIMESTAMP(6) NULL,
  PRIMARY KEY (id),
  KEY idx_restock_orders_item (inventory_item_id),
  KEY idx_restock_orders_status (status),
  KEY idx_restock_orders_created_at (created_at),
  KEY idx_restock_orders_status_created_at (status, created_at),
  CONSTRAINT fk_restock_orders_inventory_item
    FOREIGN KEY (inventory_item_id) REFERENCES inventory_items (id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,
  CONSTRAINT chk_restock_orders_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'RECEIVED', 'CANCELLED')),
  CONSTRAINT chk_restock_orders_quantity CHECK (requested_quantity > 0),
  CONSTRAINT chk_restock_orders_cost CHECK (estimated_cost >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS activity_logs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  type VARCHAR(40) NOT NULL,
  message VARCHAR(500) NOT NULL,
  item_sku VARCHAR(80),
  quantity INT,
  last_restock_date DATE,
  actor_email VARCHAR(160) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_activity_logs_created_at (created_at),
  KEY idx_activity_logs_item_sku (item_sku),
  KEY idx_activity_logs_type_created_at (type, created_at),
  CONSTRAINT chk_activity_logs_type CHECK (
    type IN (
      'ITEM_CREATED',
      'ITEM_UPDATED',
      'STOCK_ADJUSTED',
      'RESTOCK_ORDERED',
      'RESTOCK_RECEIVED',
      'MANAGER_UPDATED',
      'NOTE'
    )
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
