-- Manual additive schema proposal for the custom 3D print workflow.
-- NOT RUN. Confirm the live MariaDB definitions and back up the database first.
-- On MariaDB 10.4, inspect SHOW COLUMNS before each ALTER and omit columns already present.

ALTER TABLE design_files
  ADD COLUMN mime_type VARCHAR(190) NULL,
  ADD COLUMN file_size_bytes BIGINT UNSIGNED NULL,
  ADD COLUMN sha256_hash CHAR(64) NULL,
  ADD COLUMN deleted_at DATETIME NULL,
  ADD COLUMN analysis_status VARCHAR(32) NULL,
  ADD COLUMN source_unit VARCHAR(8) NULL DEFAULT 'mm',
  ADD COLUMN width_mm DECIMAL(12,4) NULL,
  ADD COLUMN depth_mm DECIMAL(12,4) NULL,
  ADD COLUMN height_mm DECIMAL(12,4) NULL,
  ADD COLUMN volume_cm3 DECIMAL(18,6) NULL,
  ADD COLUMN surface_area_cm2 DECIMAL(18,6) NULL,
  ADD COLUMN triangle_count BIGINT UNSIGNED NULL,
  ADD COLUMN analysis_message VARCHAR(500) NULL;

ALTER TABLE print_shops
  ADD COLUMN contact_email VARCHAR(190) NULL,
  ADD COLUMN address_line VARCHAR(255) NULL,
  ADD COLUMN capabilities TEXT NULL,
  ADD COLUMN accepted_file_formats VARCHAR(255) NULL,
  ADD COLUMN max_build_dimensions VARCHAR(120) NULL,
  ADD COLUMN operating_hours VARCHAR(255) NULL,
  ADD COLUMN estimated_lead_days SMALLINT NULL,
  ADD COLUMN service_notes TEXT NULL,
  ADD COLUMN maximum_custom_print_quantity INT UNSIGNED NOT NULL DEFAULT 100000,
  ADD COLUMN latitude DECIMAL(10,7) NULL,
  ADD COLUMN longitude DECIMAL(10,7) NULL;

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

CREATE TABLE IF NOT EXISTS shop_print_quantity_discount_tiers (
  discount_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  profile_id BIGINT NOT NULL,
  minimum_quantity INT UNSIGNED NOT NULL,
  discount_rate DECIMAL(6,5) NOT NULL,
  UNIQUE KEY uq_shop_print_discount (profile_id, minimum_quantity),
  CONSTRAINT fk_shop_print_discount_profile FOREIGN KEY (profile_id)
    REFERENCES shop_print_price_profiles(profile_id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS custom_print_requests (
  custom_request_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  design_file_id BIGINT NOT NULL,
  shop_id BIGINT NULL,
  request_code VARCHAR(40) NOT NULL UNIQUE,
  workflow_type VARCHAR(16) NULL,
  printing_method VARCHAR(100) NULL,
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
  order_id BIGINT NULL UNIQUE,
  submitted_at DATETIME NULL,
  quoted_at DATETIME NULL,
  accepted_at DATETIME NULL,
  declined_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_custom_request_file (design_file_id),
  KEY idx_custom_request_shop_status (shop_id, status, created_at),
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

CREATE TABLE IF NOT EXISTS custom_print_request_history (
  history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  custom_request_id BIGINT NOT NULL,
  changed_by_user_id BIGINT NULL,
  old_status VARCHAR(40) NULL,
  new_status VARCHAR(40) NOT NULL,
  note VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_custom_request_history (custom_request_id, created_at),
  CONSTRAINT fk_custom_request_history_request FOREIGN KEY (custom_request_id)
    REFERENCES custom_print_requests(custom_request_id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_custom_request_history_user FOREIGN KEY (changed_by_user_id)
    REFERENCES users(user_id) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
