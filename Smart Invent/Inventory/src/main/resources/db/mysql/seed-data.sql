-- Smart Invent MySQL seed data
-- Seeded user records store BCrypt password hashes, not plain text passwords.
-- Run schema.sql before this file.

USE stockwise_inventory;

INSERT INTO user_accounts (
  id,
  name,
  email,
  password_hash,
  role,
  active,
  email_verified,
  verified_at,
  last_login_at,
  created_at,
  updated_at
) VALUES
  (1, 'Smart Invent Admin', 'admin@smartinvent.local', '$2a$10$xJ97c/8tCXbPDA6KofAm4uyyofUSkzXtp5sC46i4G1gUCJzzj2SiC', 'ADMIN', TRUE, TRUE, CURRENT_TIMESTAMP(6), NULL, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
  (2, 'Operations Manager', 'manager@smartinvent.local', '$2a$10$f/CsiCP0PoJ.yOk4VQfpGO4TRxuTa/qy963zFEpYlawQQhrAMlg.K', 'MANAGER', TRUE, TRUE, CURRENT_TIMESTAMP(6), NULL, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
  (3, 'Inventory Viewer', 'viewer@smartinvent.local', '$2a$10$PCRzEheRXGEjoGD6iIDbIeBXIE36RI51JMzz0eEDVX7Z437yL/6cm', 'VIEWER', TRUE, TRUE, CURRENT_TIMESTAMP(6), NULL, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)) AS incoming
ON DUPLICATE KEY UPDATE
  name = incoming.name,
  password_hash = incoming.password_hash,
  role = incoming.role,
  active = incoming.active,
  email_verified = incoming.email_verified,
  verified_at = COALESCE(user_accounts.verified_at, incoming.verified_at),
  updated_at = CURRENT_TIMESTAMP(6);

INSERT INTO user_profiles (
  id,
  user_account_id,
  display_name,
  phone,
  department,
  job_title,
  location,
  bio,
  created_at,
  updated_at
) VALUES
  (1, 1, 'Smart Invent Admin', '555-0190', 'Operations', 'System Administrator', 'Main Office', 'Owns account security, user access, and operating controls.', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
  (2, 2, 'Operations Manager', '555-0191', 'Operations', 'Operations Manager', 'Store Floor', 'Manages inventory movement, restock follow-up, and daily exceptions.', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
  (3, 3, 'Inventory Viewer', '555-0192', 'Inventory', 'Inventory Viewer', 'Back Office', 'Reviews stock dashboards, activity history, and manager coverage.', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)) AS incoming
ON DUPLICATE KEY UPDATE
  display_name = incoming.display_name,
  phone = incoming.phone,
  department = incoming.department,
  job_title = incoming.job_title,
  location = incoming.location,
  bio = incoming.bio,
  updated_at = CURRENT_TIMESTAMP(6);

INSERT INTO store_managers (
  id,
  full_name,
  title,
  department,
  email,
  phone,
  shift_name,
  status,
  responsibilities,
  created_at,
  updated_at
) VALUES
  (1, 'Elena Silva', 'Operations Lead', 'Operations', 'elena@smartinvent.local', '555-0100', 'Full day', 'Available', 'Exception review, supplier escalation, and daily operating rhythm', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
  (2, 'Mira Patel', 'Grocery Manager', 'Grocery', 'mira@smartinvent.local', '555-0101', 'Morning', 'Available', 'Fresh grocery checks, aisle stock health, and replenishment approvals', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
  (3, 'Noah Chen', 'Home Goods Manager', 'Home Goods', 'noah@smartinvent.local', '555-0102', 'Afternoon', 'In review', 'Home goods cycle counts, damaged stock review, and shelf capacity planning', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
  (4, 'Tara Hughes', 'Body Care Manager', 'Body Care', 'tara@smartinvent.local', '555-0103', 'Morning', 'Available', 'Body care assortment, vendor follow-ups, and low-stock exceptions', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
  (5, 'Owen Brooks', 'Lifestyle Manager', 'Lifestyle', 'owen@smartinvent.local', '555-0104', 'Afternoon', 'Available', 'Seasonal lifestyle inventory, display readiness, and stockroom rotation', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
  (6, 'Priya Nair', 'Stationery Manager', 'Stationery', 'priya@smartinvent.local', '555-0105', 'Evening', 'Available', 'Stationery replenishment, back-to-school readiness, and shrinkage checks', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
  (7, 'Marcus Reed', 'Apparel Manager', 'Apparel', 'marcus@smartinvent.local', '555-0106', 'Morning', 'Follow-up', 'Apparel rack coverage, sizing gaps, and returns monitoring', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)) AS incoming
ON DUPLICATE KEY UPDATE
  full_name = incoming.full_name,
  title = incoming.title,
  department = incoming.department,
  phone = incoming.phone,
  shift_name = incoming.shift_name,
  status = incoming.status,
  responsibilities = incoming.responsibilities,
  updated_at = CURRENT_TIMESTAMP(6);

INSERT INTO inventory_items (
  id,
  sku,
  name,
  category,
  supplier,
  quantity,
  reorder_point,
  unit_cost,
  retail_price,
  lead_time_days,
  location,
  notes,
  last_restock_date,
  last_restock_quantity,
  created_at,
  updated_at
) VALUES
  (1, 'ITEM-0001', 'Colombian Whole Bean Coffee', 'Grocery', 'Andes Roasters', 18, 24, 6.90, 12.99, 5, 'Aisle 1', 'Best seller, monitor weekend demand', '2026-05-19', 36, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
  (2, 'ITEM-0002', 'Organic Lavender Soap', 'Body Care', 'Botanic Works', 0, 18, 2.10, 5.49, 7, 'Shelf C2', 'Out of stock until next order', '2026-05-10', 24, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
  (3, 'ITEM-0003', 'Reusable Produce Bags', 'Home Goods', 'EcoSupply Co', 42, 20, 3.45, 8.99, 6, 'Aisle 4', 'High margin add-on item', '2026-05-23', 40, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
  (4, 'ITEM-0004', 'Local Honey Jar', 'Grocery', 'Meadow Apiary', 11, 16, 4.20, 9.99, 3, 'Aisle 2', 'Fast moving local product', '2026-05-18', 20, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
  (5, 'ITEM-0005', 'Premium Gel Pen Pack', 'Stationery', 'Northline Paper', 64, 20, 1.80, 4.99, 4, 'Shelf S1', 'Keep near checkout', '2026-05-24', 60, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
  (6, 'ITEM-0006', 'Kids Rain Poncho', 'Apparel', 'Trailwear Goods', 7, 14, 5.95, 14.99, 12, 'Rack 4', 'Seasonal weather spike risk', '2026-05-12', 18, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
  (7, 'ITEM-0007', 'Soy Candle Citrus', 'Lifestyle', 'Glow Studio', 38, 12, 4.75, 11.99, 8, 'Display L3', 'Strong gifting item', '2026-05-22', 24, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
  (8, 'ITEM-0008', 'Stoneware Mug Set', 'Home Goods', 'Clay & Co', 23, 10, 9.50, 24.99, 9, 'Aisle 5', 'Fragile stock, inspect cartons', '2026-05-20', 16, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)) AS incoming
ON DUPLICATE KEY UPDATE
  name = incoming.name,
  category = incoming.category,
  supplier = incoming.supplier,
  quantity = incoming.quantity,
  reorder_point = incoming.reorder_point,
  unit_cost = incoming.unit_cost,
  retail_price = incoming.retail_price,
  lead_time_days = incoming.lead_time_days,
  location = incoming.location,
  notes = incoming.notes,
  last_restock_date = incoming.last_restock_date,
  last_restock_quantity = incoming.last_restock_quantity,
  updated_at = CURRENT_TIMESTAMP(6);

INSERT INTO restock_orders (
  id,
  inventory_item_id,
  requested_quantity,
  estimated_cost,
  status,
  requested_by,
  created_at,
  updated_at,
  received_at
) VALUES
  (1, 2, 36, 75.60, 'SUBMITTED', 'admin@smartinvent.local', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL),
  (2, 6, 21, 124.95, 'DRAFT', 'manager@smartinvent.local', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL) AS incoming
ON DUPLICATE KEY UPDATE
  inventory_item_id = incoming.inventory_item_id,
  requested_quantity = incoming.requested_quantity,
  estimated_cost = incoming.estimated_cost,
  status = incoming.status,
  requested_by = incoming.requested_by,
  updated_at = CURRENT_TIMESTAMP(6),
  received_at = incoming.received_at;

INSERT INTO activity_logs (
  id,
  type,
  message,
  item_sku,
  quantity,
  last_restock_date,
  actor_email,
  created_at
) VALUES
  (1, 'NOTE', 'Backend initialized with seed operating data', NULL, NULL, NULL, 'system', CURRENT_TIMESTAMP(6)),
  (2, 'RESTOCK_ORDERED', 'Review required for Organic Lavender Soap', 'ITEM-0002', 24, '2026-05-10', 'system', CURRENT_TIMESTAMP(6)),
  (3, 'STOCK_ADJUSTED', 'Low stock watch opened for Kids Rain Poncho', 'ITEM-0006', 7, '2026-05-12', 'system', CURRENT_TIMESTAMP(6)) AS incoming
ON DUPLICATE KEY UPDATE
  type = incoming.type,
  message = incoming.message,
  item_sku = incoming.item_sku,
  quantity = incoming.quantity,
  last_restock_date = incoming.last_restock_date,
  actor_email = incoming.actor_email;

ALTER TABLE user_accounts AUTO_INCREMENT = 4;
ALTER TABLE user_profiles AUTO_INCREMENT = 4;
ALTER TABLE email_verification_codes AUTO_INCREMENT = 1;
ALTER TABLE store_managers AUTO_INCREMENT = 8;
ALTER TABLE inventory_items AUTO_INCREMENT = 9;
ALTER TABLE restock_orders AUTO_INCREMENT = 3;
ALTER TABLE activity_logs AUTO_INCREMENT = 4;
