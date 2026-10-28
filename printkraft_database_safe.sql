-- PrintKraft MySQL Database Schema (SAFE SETUP COPY)
-- Generated automatically from ERD
--
-- This is a documented safe copy of printkraft_database.sql (original preserved untouched).
-- Differences from the original file:
--   1. The destructive statement `DROP DATABASE IF EXISTS printkraft` is removed so
--      importing this script can never destroy an existing database. If the database
--      already exists, its data is kept (CREATE DATABASE IF NOT EXISTS).
--   2. `product_filter_values.product_id` and `product_filter_values.option_id` no
--      longer declare AUTO_INCREMENT. MySQL allows only ONE auto-increment column per
--      table; both columns reference existing products / filter options and are part
--      of the composite primary key, so they must be plain NOT NULL BIGINTs.
--
-- Safe first-time setup:
--   mysql -u root -p < printkraft_database_safe.sql

SET FOREIGN_KEY_CHECKS=0;
CREATE DATABASE IF NOT EXISTS printkraft CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE printkraft;

-- Table: users
-- Description: Tài khoản khách hàng, chủ xưởng, quản trị viên
CREATE TABLE `users` (
  `user_id` BIGINT AUTO_INCREMENT,
  `full_name` VARCHAR(120),
  `email` VARCHAR(190) UNIQUE,
  `phone` VARCHAR(25),
  `password_hash` VARCHAR(255),
  `role` VARCHAR(50),
  `status` VARCHAR(50),
  `created_at` DATETIME,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: addresses
-- Description: Địa chỉ nhận hàng đã lưu
CREATE TABLE `addresses` (
  `address_id` BIGINT AUTO_INCREMENT,
  `user_id` BIGINT,
  `recipient_name` VARCHAR(120),
  `recipient_phone` VARCHAR(25),
  `province` VARCHAR(100),
  `district` VARCHAR(100),
  `address_line` VARCHAR(255),
  `is_default` BOOLEAN,
  PRIMARY KEY (`address_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: carts
-- Description: Giỏ hàng đang hoạt động của tài khoản khách hàng
CREATE TABLE `carts` (
  `cart_id` BIGINT AUTO_INCREMENT,
  `user_id` BIGINT UNIQUE,
  `status` VARCHAR(50),
  `currency` CHAR(3),
  `subtotal` DECIMAL(12,2),
  `expires_at` DATETIME,
  `created_at` DATETIME,
  `updated_at` DATETIME,
  PRIMARY KEY (`cart_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: cart_items
-- Description: Sản phẩm/xưởng/thiết kế được chọn trong giỏ
CREATE TABLE `cart_items` (
  `cart_item_id` BIGINT AUTO_INCREMENT,
  `cart_id` BIGINT,
  `shop_product_id` BIGINT,
  `design_file_id` BIGINT,
  `quantity` INT,
  `selected_options` JSON,
  `unit_price_snapshot` DECIMAL(12,2),
  `line_total` DECIMAL(12,2),
  `created_at` DATETIME,
  PRIMARY KEY (`cart_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: favorites
-- Description: Danh sách sản phẩm yêu thích
CREATE TABLE `favorites` (
  `favorite_id` BIGINT AUTO_INCREMENT,
  `user_id` BIGINT,
  `product_id` BIGINT,
  `created_at` DATETIME,
  PRIMARY KEY (`favorite_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: categories
-- Description: Danh mục sản phẩm có thể phân cấp
CREATE TABLE `categories` (
  `category_id` BIGINT AUTO_INCREMENT,
  `parent_id` BIGINT,
  `category_name` VARCHAR(120),
  `slug` VARCHAR(150) UNIQUE,
  `is_active` BOOLEAN,
  `display_order` INT,
  PRIMARY KEY (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: products
-- Description: Thông tin chi tiết sản phẩm trong catalog
CREATE TABLE `products` (
  `product_id` BIGINT AUTO_INCREMENT,
  `category_id` BIGINT,
  `product_name` VARCHAR(180),
  `slug` VARCHAR(200) UNIQUE,
  `description` TEXT,
  `brand` VARCHAR(120),
  `base_unit` VARCHAR(30),
  `is_customizable` BOOLEAN,
  `is_active` BOOLEAN,
  `search_keywords` VARCHAR(500),
  `created_at` DATETIME,
  `updated_at` DATETIME,
  PRIMARY KEY (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: product_images
-- Description: Ảnh và nội dung trực quan khi xem chi tiết sản phẩm
CREATE TABLE `product_images` (
  `image_id` BIGINT AUTO_INCREMENT,
  `product_id` BIGINT,
  `image_url` VARCHAR(500),
  `alt_text` VARCHAR(255),
  `is_primary` BOOLEAN,
  `display_order` INT,
  PRIMARY KEY (`image_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: product_filters
-- Description: Tiêu chí lọc sản phẩm như màu, chất liệu, khoảng giá
CREATE TABLE `product_filters` (
  `filter_id` BIGINT AUTO_INCREMENT,
  `filter_code` VARCHAR(60) UNIQUE,
  `filter_name` VARCHAR(120),
  `filter_type` VARCHAR(50),
  `display_order` INT,
  `is_active` BOOLEAN,
  PRIMARY KEY (`filter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: product_filter_options
-- Description: Các lựa chọn hiển thị cho từng tiêu chí lọc
CREATE TABLE `product_filter_options` (
  `option_id` BIGINT AUTO_INCREMENT,
  `filter_id` BIGINT,
  `option_label` VARCHAR(120),
  `option_value` VARCHAR(120),
  `display_order` INT,
  `is_active` BOOLEAN,
  PRIMARY KEY (`option_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: product_filter_values
-- Description: Liên kết sản phẩm với các lựa chọn dùng để lọc
CREATE TABLE `product_filter_values` (
  `product_id` BIGINT NOT NULL,
  `option_id` BIGINT NOT NULL,
  `value_text` VARCHAR(255),
  `numeric_value` DECIMAL(12,2),
  PRIMARY KEY (`product_id`, `option_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: print_shops
-- Description: Gian hàng có thể tìm kiếm theo tên, khu vực và trạng thái
CREATE TABLE `print_shops` (
  `shop_id` BIGINT AUTO_INCREMENT,
  `owner_user_id` BIGINT,
  `shop_name` VARCHAR(180),
  `slug` VARCHAR(200) UNIQUE,
  `description` TEXT,
  `logo_url` VARCHAR(500),
  `contact_phone` VARCHAR(25),
  `province` VARCHAR(100),
  `district` VARCHAR(100),
  `search_keywords` VARCHAR(500),
  `average_rating` DECIMAL(3,2),
  `verification_status` VARCHAR(50),
  `status` VARCHAR(50),
  `created_at` DATETIME,
  PRIMARY KEY (`shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: shop_products
-- Description: Chào bán cụ thể để xem giá, vật liệu và thời gian sản xuất
CREATE TABLE `shop_products` (
  `shop_product_id` BIGINT AUTO_INCREMENT,
  `shop_id` BIGINT,
  `product_id` BIGINT,
  `sku` VARCHAR(80) UNIQUE,
  `printing_method` VARCHAR(100),
  `material` VARCHAR(120),
  `unit_price` DECIMAL(12,2),
  `minimum_quantity` INT,
  `production_days` SMALLINT,
  `stock_status` VARCHAR(50),
  `is_active` BOOLEAN,
  `updated_at` DATETIME,
  PRIMARY KEY (`shop_product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: design_files
-- Description: File thiết kế do khách tải lên để cá nhân hóa
CREATE TABLE `design_files` (
  `design_file_id` BIGINT AUTO_INCREMENT,
  `user_id` BIGINT,
  `product_id` BIGINT,
  `original_filename` VARCHAR(255),
  `storage_url` VARCHAR(800),
  `preview_url` VARCHAR(800),
  `status` VARCHAR(50),
  `created_at` DATETIME,
  PRIMARY KEY (`design_file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: orders
-- Description: Đơn hàng, trạng thái thanh toán và lịch sử mua hàng
CREATE TABLE `orders` (
  `order_id` BIGINT AUTO_INCREMENT,
  `order_code` VARCHAR(40) UNIQUE,
  `user_id` BIGINT,
  `address_id` BIGINT,
  `recipient_name_snapshot` VARCHAR(120) NOT NULL,
  `recipient_phone_snapshot` VARCHAR(25) NOT NULL,
  `shipping_address_snapshot` VARCHAR(500) NOT NULL,
  `subtotal` DECIMAL(12,2),
  `shipping_fee` DECIMAL(12,2),
  `total_amount` DECIMAL(12,2),
  `currency` CHAR(3),
  `status` VARCHAR(50),
  `payment_status` VARCHAR(50),
  `shipping_status` VARCHAR(50),
  `placed_at` DATETIME,
  `completed_at` DATETIME,
  `cancelled_at` DATETIME,
  PRIMARY KEY (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: order_items
-- Description: Dòng sản phẩm đã mua, giữ snapshot tên, gian hàng và giá
CREATE TABLE `order_items` (
  `order_item_id` BIGINT AUTO_INCREMENT,
  `order_id` BIGINT,
  `product_id` BIGINT,
  `shop_product_id` BIGINT,
  `shop_id` BIGINT,
  `design_file_id` BIGINT,
  `product_name_snapshot` VARCHAR(180),
  `shop_name_snapshot` VARCHAR(180),
  `quantity` INT,
  `unit_price` DECIMAL(12,2),
  `line_total` DECIMAL(12,2),
  `status` VARCHAR(50),
  PRIMARY KEY (`order_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: payments
-- Description: Các lần thử thanh toán trực tuyến và giao dịch thành công
CREATE TABLE `payments` (
  `payment_id` BIGINT AUTO_INCREMENT,
  `order_id` BIGINT,
  `payment_method` VARCHAR(50),
  `provider` VARCHAR(60),
  `provider_transaction_ref` VARCHAR(190) UNIQUE,
  `checkout_url` VARCHAR(800),
  `amount` DECIMAL(12,2),
  `status` VARCHAR(50),
  `failure_reason` VARCHAR(500),
  `paid_at` DATETIME,
  `created_at` DATETIME,
  `updated_at` DATETIME,
  PRIMARY KEY (`payment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: quote_requests
-- Description: Yêu cầu báo giá cho đơn doanh nghiệp/số lượng lớn
CREATE TABLE `quote_requests` (
  `quote_request_id` BIGINT AUTO_INCREMENT,
  `user_id` BIGINT,
  `product_id` BIGINT,
  `preferred_shop_id` BIGINT,
  `company_name` VARCHAR(180),
  `requested_quantity` INT,
  `required_by` DATE,
  `quoted_amount` DECIMAL(12,2),
  `status` VARCHAR(50),
  PRIMARY KEY (`quote_request_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: order_status_history
-- Description: Dòng thời gian để xem lịch sử mua hàng và trạng thái đơn
CREATE TABLE `order_status_history` (
  `history_id` BIGINT AUTO_INCREMENT,
  `order_id` BIGINT,
  `changed_by_user_id` BIGINT,
  `old_status` VARCHAR(40),
  `new_status` VARCHAR(40),
  `note` VARCHAR(500),
  `changed_at` DATETIME,
  PRIMARY KEY (`history_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: production_updates
-- Description: Tiến độ sản xuất từ xưởng in
CREATE TABLE `production_updates` (
  `production_update_id` BIGINT AUTO_INCREMENT,
  `order_item_id` BIGINT,
  `shop_id` BIGINT,
  `created_by_user_id` BIGINT,
  `stage` VARCHAR(50),
  `progress_percent` TINYINT,
  `message` TEXT,
  `created_at` DATETIME,
  PRIMARY KEY (`production_update_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: shipments
-- Description: Vận chuyển và mã theo dõi; hỗ trợ nhiều xưởng/đơn
CREATE TABLE `shipments` (
  `shipment_id` BIGINT AUTO_INCREMENT,
  `order_id` BIGINT,
  `shop_id` BIGINT,
  `carrier` VARCHAR(100),
  `tracking_number` VARCHAR(120),
  `status` VARCHAR(50),
  `shipped_at` DATETIME,
  `delivered_at` DATETIME,
  PRIMARY KEY (`shipment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: reviews
-- Description: Đánh giá sau khi mua, gắn với dòng hàng thực tế
CREATE TABLE `reviews` (
  `review_id` BIGINT AUTO_INCREMENT,
  `order_item_id` BIGINT UNIQUE,
  `user_id` BIGINT,
  `product_id` BIGINT,
  `shop_id` BIGINT,
  `rating` TINYINT,
  `review_text` TEXT,
  `is_published` BOOLEAN,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`review_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS=1;
