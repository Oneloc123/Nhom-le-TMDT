-- PrintKraft custom 3D print workflow: reviewed additive schema patch.
-- Target: MariaDB 10.4. This file is a proposal for the operator to run manually.
-- It has not been executed by the application agent and must not be pointed at
-- a production database without the operator first selecting and inspecting it.
--
-- Do NOT import printkraft_database.sql over an existing database: that bootstrap
-- file contains DROP DATABASE. printkraft_database_safe.sql is for first-time
-- setup; its print_shops columns are already handled below with IF NOT EXISTS.
-- This one patch supports either repository schema. MariaDB 10.4 supports
-- ADD COLUMN/INDEX IF NOT EXISTS, so existing safe-schema columns are not added
-- a second time. Existing columns with an unexpected definition still need a
-- manual review; IF NOT EXISTS does not repair or validate a mismatched column.
--
-- Preflight (run manually, inspect output, then select the intended database):
--   SELECT VERSION(), DATABASE();
--   SHOW CREATE TABLE users;
--   SHOW CREATE TABLE design_files;
--   SHOW CREATE TABLE print_shops;
--   SHOW CREATE TABLE orders;
--   SHOW CREATE TABLE order_items;
--   SHOW CREATE TABLE order_status_history;
--   SHOW CREATE TABLE production_updates;
--   SHOW CREATE TABLE reviews;
--   SHOW INDEX FROM orders;
--   SHOW INDEX FROM order_items;
-- Confirm all parent tables and their IDs are InnoDB BIGINT, and that the
-- selected schema is the intended PrintKraft database. Back up the database.
-- If custom-print tables already exist, inspect their full definitions and data
-- before continuing; CREATE TABLE IF NOT EXISTS will not repair partial tables.
-- If adding a unique key reports duplicates, stop and resolve them deliberately.
-- No DROP TABLE, DROP COLUMN, or data deletion is used in this patch.

-- 1. File metadata and server-side model analysis written/read by DesignFileDao.
ALTER TABLE design_files
  ADD COLUMN IF NOT EXISTS mime_type VARCHAR(190) NULL,
  ADD COLUMN IF NOT EXISTS file_size_bytes BIGINT UNSIGNED NULL,
  ADD COLUMN IF NOT EXISTS sha256_hash CHAR(64) NULL,
  ADD COLUMN IF NOT EXISTS deleted_at DATETIME NULL,
  ADD COLUMN IF NOT EXISTS analysis_status VARCHAR(32) NULL,
  ADD COLUMN IF NOT EXISTS source_unit VARCHAR(8) NULL DEFAULT 'mm',
  ADD COLUMN IF NOT EXISTS width_mm DECIMAL(12,4) NULL,
  ADD COLUMN IF NOT EXISTS depth_mm DECIMAL(12,4) NULL,
  ADD COLUMN IF NOT EXISTS height_mm DECIMAL(12,4) NULL,
  ADD COLUMN IF NOT EXISTS volume_cm3 DECIMAL(18,6) NULL,
  ADD COLUMN IF NOT EXISTS surface_area_cm2 DECIMAL(18,6) NULL,
  ADD COLUMN IF NOT EXISTS triangle_count BIGINT UNSIGNED NULL,
  ADD COLUMN IF NOT EXISTS analysis_message VARCHAR(500) NULL;

-- 2. Workshop contact, capability, build-volume, quantity, and distance fields.
--    contact_email/address_line/capabilities/accepted_file_formats/
--    max_build_dimensions/operating_hours/estimated_lead_days/service_notes are
--    already present in printkraft_database_safe.sql; IF NOT EXISTS preserves them.
ALTER TABLE print_shops
  ADD COLUMN IF NOT EXISTS contact_email VARCHAR(190) NULL,
  ADD COLUMN IF NOT EXISTS address_line VARCHAR(255) NULL,
  ADD COLUMN IF NOT EXISTS capabilities TEXT NULL,
  ADD COLUMN IF NOT EXISTS accepted_file_formats VARCHAR(255) NULL,
  ADD COLUMN IF NOT EXISTS max_build_dimensions VARCHAR(120) NULL,
  ADD COLUMN IF NOT EXISTS operating_hours VARCHAR(255) NULL,
  ADD COLUMN IF NOT EXISTS estimated_lead_days SMALLINT NULL,
  ADD COLUMN IF NOT EXISTS service_notes TEXT NULL,
  ADD COLUMN IF NOT EXISTS maximum_custom_print_quantity INT UNSIGNED NOT NULL DEFAULT 100000,
  ADD COLUMN IF NOT EXISTS latitude DECIMAL(10,7) NULL,
  ADD COLUMN IF NOT EXISTS longitude DECIMAL(10,7) NULL;

-- 3. Order fields used by the current checkout, account, tracking, and cancel
--    DAOs. Both repository bootstrap schemas omit these additions. A nullable,
--    unique custom_print_request_id is the database-level one-order-per-request
--    guard; ordinary orders keep NULL and remain unrestricted.
ALTER TABLE orders
  ADD COLUMN IF NOT EXISTS shipping_latitude DECIMAL(10,7) NULL,
  ADD COLUMN IF NOT EXISTS shipping_longitude DECIMAL(10,7) NULL,
  ADD COLUMN IF NOT EXISTS delivery_note_snapshot VARCHAR(500) NULL,
  ADD COLUMN IF NOT EXISTS customer_note VARCHAR(1000) NULL,
  ADD COLUMN IF NOT EXISTS cancel_reason VARCHAR(500) NULL,
  ADD COLUMN IF NOT EXISTS tracking_token CHAR(64) NULL,
  ADD COLUMN IF NOT EXISTS tracking_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS estimated_delivery_at DATETIME NULL,
  ADD COLUMN IF NOT EXISTS custom_print_request_id BIGINT NULL;

ALTER TABLE orders
  ADD UNIQUE INDEX IF NOT EXISTS uq_orders_custom_print_request (custom_print_request_id),
  ADD INDEX IF NOT EXISTS idx_orders_user_placed (user_id, placed_at, order_id);

-- 4. Supporting indexes for owner/file lookups, order history, and the
--    latest production progress lookup. Existing indexes are retained.
ALTER TABLE design_files
  ADD INDEX IF NOT EXISTS idx_design_files_owner_deleted (user_id, deleted_at);
ALTER TABLE print_shops
  ADD INDEX IF NOT EXISTS idx_print_shops_owner_user (owner_user_id),
  ADD INDEX IF NOT EXISTS idx_print_shops_workflow_status (verification_status, status);
ALTER TABLE order_items
  ADD INDEX IF NOT EXISTS idx_order_items_order_id (order_id);
ALTER TABLE order_status_history
  ADD INDEX IF NOT EXISTS idx_order_status_history_order_time (order_id, changed_at, history_id);
ALTER TABLE production_updates
  ADD INDEX IF NOT EXISTS idx_production_updates_item_shop_time
    (order_item_id, shop_id, created_at, production_update_id);
ALTER TABLE reviews
  ADD INDEX IF NOT EXISTS idx_reviews_shop_published (shop_id, is_published);

-- 5. Shop pricing profiles. Unique shop/method/material is the lookup and
--    upsert key used by CustomPrintPricingDao.saveProfile().
CREATE TABLE IF NOT EXISTS shop_print_price_profiles (
  profile_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  shop_id BIGINT NOT NULL,
  printing_method VARCHAR(100) NOT NULL,
  material VARCHAR(120) NOT NULL,
  material_price_per_gram DECIMAL(12,4) NOT NULL,
  material_density_g_cm3 DECIMAL(10,4) NOT NULL,
  material_waste_factor DECIMAL(8,5) NOT NULL DEFAULT 0.05000,
  machine_cost_per_hour DECIMAL(12,2) NOT NULL,
  deposition_rate_cm3_per_hour DECIMAL(10,4) NOT NULL,
  wall_thickness_mm DECIMAL(8,4) NOT NULL DEFAULT 1.2000,
  support_material_factor DECIMAL(8,5) NOT NULL DEFAULT 0.05000,
  infill_usage_factor DECIMAL(8,5) NOT NULL DEFAULT 1.00000,
  reference_layer_height_mm DECIMAL(8,4) NOT NULL DEFAULT 0.2000,
  technology_factor DECIMAL(8,5) NOT NULL DEFAULT 1.00000,
  complexity_factor DECIMAL(8,5) NOT NULL DEFAULT 1.00000,
  setup_hours DECIMAL(8,4) NOT NULL DEFAULT 0.0000,
  post_processing_fee DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  minimum_order_price DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  minimum_layer_height_mm DECIMAL(8,4) NOT NULL DEFAULT 0.0800,
  maximum_layer_height_mm DECIMAL(8,4) NOT NULL DEFAULT 0.4000,
  minimum_infill_percent SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  maximum_infill_percent SMALLINT UNSIGNED NOT NULL DEFAULT 100,
  production_days SMALLINT UNSIGNED NOT NULL DEFAULT 3,
  use_material_cost BOOLEAN NOT NULL DEFAULT TRUE,
  use_machine_cost BOOLEAN NOT NULL DEFAULT TRUE,
  use_infill_factor BOOLEAN NOT NULL DEFAULT TRUE,
  use_layer_height_factor BOOLEAN NOT NULL DEFAULT TRUE,
  use_technology_factor BOOLEAN NOT NULL DEFAULT TRUE,
  use_complexity_factor BOOLEAN NOT NULL DEFAULT TRUE,
  use_post_processing_fee BOOLEAN NOT NULL DEFAULT TRUE,
  use_quantity_discount BOOLEAN NOT NULL DEFAULT TRUE,
  use_minimum_price BOOLEAN NOT NULL DEFAULT TRUE,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_shop_print_profile (shop_id, printing_method, material),
  CONSTRAINT fk_shop_print_profile_shop FOREIGN KEY (shop_id)
    REFERENCES print_shops(shop_id) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Optional color and finishing selections charged during the server estimate.
CREATE TABLE IF NOT EXISTS shop_print_price_options (
  option_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  profile_id BIGINT NOT NULL,
  option_type VARCHAR(20) NOT NULL,
  option_value VARCHAR(100) NOT NULL,
  label VARCHAR(120) NOT NULL,
  fee DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  UNIQUE KEY uq_shop_print_option (profile_id, option_type, option_value),
  CONSTRAINT fk_shop_print_option_profile FOREIGN KEY (profile_id)
    REFERENCES shop_print_price_profiles(profile_id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Quantity discounts used by pricing lookup and upsert.
CREATE TABLE IF NOT EXISTS shop_print_quantity_discount_tiers (
  discount_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  profile_id BIGINT NOT NULL,
  minimum_quantity INT UNSIGNED NOT NULL,
  discount_rate DECIMAL(6,5) NOT NULL,
  UNIQUE KEY uq_shop_print_discount (profile_id, minimum_quantity),
  CONSTRAINT fk_shop_print_discount_profile FOREIGN KEY (profile_id)
    REFERENCES shop_print_price_profiles(profile_id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. One record stores both direct-order drafts and workshop quote requests.
--    `technology` is the actual column written by CustomPrintWorkflowDao;
--    `printing_method` in pricing profiles remains the separate price key.
CREATE TABLE IF NOT EXISTS custom_print_requests (
  custom_request_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  design_file_id BIGINT NOT NULL,
  shop_id BIGINT NULL,
  request_code VARCHAR(40) NOT NULL,
  workflow_type VARCHAR(16) NULL,
  technology VARCHAR(100) NULL,
  material VARCHAR(120) NULL,
  infill_percent SMALLINT UNSIGNED NULL,
  layer_height_mm DECIMAL(8,4) NULL,
  quantity INT UNSIGNED NULL,
  color_option VARCHAR(100) NULL,
  post_processing_option VARCHAR(100) NULL,
  customer_note VARCHAR(1000) NULL,
  dimensions_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
  estimated_total DECIMAL(12,2) NULL,
  quoted_amount DECIMAL(12,2) NULL,
  seller_response_note VARCHAR(500) NULL,
  production_days SMALLINT UNSIGNED NULL,
  quote_valid_until DATETIME NULL,
  status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
  order_id BIGINT NULL,
  submitted_at DATETIME NULL,
  quoted_at DATETIME NULL,
  accepted_at DATETIME NULL,
  declined_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_custom_request_code (request_code),
  UNIQUE KEY uq_custom_request_order (order_id),
  KEY idx_custom_request_user_created (user_id, created_at, custom_request_id),
  KEY idx_custom_request_file (design_file_id),
  KEY idx_custom_request_shop_status (shop_id, workflow_type, status, created_at),
  KEY idx_custom_request_expiry (status, quote_valid_until),
  CONSTRAINT fk_custom_request_user FOREIGN KEY (user_id)
    REFERENCES users(user_id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_custom_request_file FOREIGN KEY (design_file_id)
    REFERENCES design_files(design_file_id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_custom_request_shop FOREIGN KEY (shop_id)
    REFERENCES print_shops(shop_id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_custom_request_order FOREIGN KEY (order_id)
    REFERENCES orders(order_id) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add the customer-list index separately so this also upgrades the earlier
-- proposal table, whose other indexes already cover shop, file, code, order,
-- and expiry lookups. Avoid creating duplicate unique indexes on those columns.
ALTER TABLE custom_print_requests
  ADD INDEX IF NOT EXISTS idx_custom_request_user_created (user_id, created_at, custom_request_id);

-- 9. Append-only status history for both customer and workshop actions.
CREATE TABLE IF NOT EXISTS custom_print_request_history (
  history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  custom_request_id BIGINT NOT NULL,
  changed_by_user_id BIGINT NULL,
  old_status VARCHAR(40) NULL,
  new_status VARCHAR(40) NOT NULL,
  note VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_custom_request_history (custom_request_id, created_at, history_id),
  CONSTRAINT fk_custom_request_history_request FOREIGN KEY (custom_request_id)
    REFERENCES custom_print_requests(custom_request_id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_custom_request_history_user FOREIGN KEY (changed_by_user_id)
    REFERENCES users(user_id) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Add the reverse link after both tables exist. Together with
--     custom_print_requests.order_id UNIQUE this prevents either side of a
--     custom request/order relationship from being reused. The app transaction
--     still locks the request and rolls back the order if its state update loses.
SET @pk_custom_request_fk_exists := (
  SELECT COUNT(*)
  FROM information_schema.KEY_COLUMN_USAGE
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'orders'
    AND COLUMN_NAME = 'custom_print_request_id'
    AND REFERENCED_TABLE_NAME = 'custom_print_requests'
    AND REFERENCED_COLUMN_NAME = 'custom_request_id'
);
SET @pk_custom_request_fk_sql := IF(
  @pk_custom_request_fk_exists = 0,
  'ALTER TABLE `orders` ADD CONSTRAINT `fk_orders_custom_print_request` FOREIGN KEY (`custom_print_request_id`) REFERENCES `custom_print_requests` (`custom_request_id`) ON UPDATE CASCADE ON DELETE RESTRICT',
  'SELECT 1'
);
PREPARE pk_custom_request_fk_stmt FROM @pk_custom_request_fk_sql;
EXECUTE pk_custom_request_fk_stmt;
DEALLOCATE PREPARE pk_custom_request_fk_stmt;

-- Postflight: inspect the final columns, keys, and constraints after the operator
-- has run this file. These are read-only checks and do not alter application data.
SHOW COLUMNS FROM design_files;
SHOW COLUMNS FROM print_shops;
SHOW COLUMNS FROM orders;
SHOW COLUMNS FROM order_items;
SHOW INDEX FROM custom_print_requests;
SHOW INDEX FROM orders;
SHOW CREATE TABLE shop_print_price_profiles;
SHOW CREATE TABLE custom_print_requests;
SHOW CREATE TABLE custom_print_request_history;
