-- Bổ sung trạng thái đặt cọc / sản xuất cho quote_requests, custom_print_requests
-- và các bảng giao dịch ZaloPay Sandbox (quote_payments, custom_print_payments).
-- Script idempotent: có thể chạy nhiều lần trên MySQL/MariaDB.

DELIMITER $$

DROP PROCEDURE IF EXISTS add_printkraft_quote_column_if_missing$$
CREATE PROCEDURE add_printkraft_quote_column_if_missing(
    IN table_name_value VARCHAR(64),
    IN column_name_value VARCHAR(64),
    IN definition_value VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = table_name_value
          AND column_name = column_name_value
    ) THEN
        SET @alter_statement = CONCAT(
            'ALTER TABLE `', REPLACE(table_name_value, '`', '``'),
            '` ADD COLUMN `', REPLACE(column_name_value, '`', '``'),
            '` ', definition_value
        );
        PREPARE migration_statement FROM @alter_statement;
        EXECUTE migration_statement;
        DEALLOCATE PREPARE migration_statement;
    END IF;
END$$

CALL add_printkraft_quote_column_if_missing('quote_requests', 'deposit_amount', 'DECIMAL(12,2) NULL AFTER `quoted_amount`')$$
CALL add_printkraft_quote_column_if_missing('quote_requests', 'deposit_paid_at', 'DATETIME NULL AFTER `deposit_amount`')$$
CALL add_printkraft_quote_column_if_missing('quote_requests', 'production_started_at', 'DATETIME NULL AFTER `deposit_paid_at`')$$
CALL add_printkraft_quote_column_if_missing('quote_requests', 'completed_at', 'DATETIME NULL AFTER `production_started_at`')$$
CALL add_printkraft_quote_column_if_missing('quote_requests', 'production_note', 'VARCHAR(500) NULL AFTER `completed_at`')$$
CALL add_printkraft_quote_column_if_missing('quote_requests', 'order_id', 'BIGINT NULL AFTER `production_note`')$$

CALL add_printkraft_quote_column_if_missing('custom_print_requests', 'quoted_at', 'DATETIME NULL AFTER `submitted_at`')$$
CALL add_printkraft_quote_column_if_missing('custom_print_requests', 'accepted_at', 'DATETIME NULL AFTER `quoted_at`')$$
CALL add_printkraft_quote_column_if_missing('custom_print_requests', 'declined_at', 'DATETIME NULL AFTER `accepted_at`')$$
CALL add_printkraft_quote_column_if_missing('custom_print_requests', 'deposit_amount', 'DECIMAL(12,2) NULL AFTER `quoted_amount`')$$
CALL add_printkraft_quote_column_if_missing('custom_print_requests', 'deposit_paid_at', 'DATETIME NULL AFTER `deposit_amount`')$$
CALL add_printkraft_quote_column_if_missing('custom_print_requests', 'production_started_at', 'DATETIME NULL AFTER `deposit_paid_at`')$$
CALL add_printkraft_quote_column_if_missing('custom_print_requests', 'completed_at', 'DATETIME NULL AFTER `production_started_at`')$$
CALL add_printkraft_quote_column_if_missing('custom_print_requests', 'production_note', 'VARCHAR(500) NULL AFTER `completed_at`')$$

DROP PROCEDURE add_printkraft_quote_column_if_missing$$

DELIMITER ;

CREATE TABLE IF NOT EXISTS `quote_payments` (
  `quote_payment_id` BIGINT AUTO_INCREMENT,
  `quote_request_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `provider` VARCHAR(40) NOT NULL DEFAULT 'ZALOPAY',
  `app_trans_id` VARCHAR(64) NOT NULL UNIQUE,
  `zp_trans_token` VARCHAR(255) NULL,
  `zp_trans_id` VARCHAR(100) NULL,
  `order_url` VARCHAR(1000) NULL,
  `amount` DECIMAL(12,2) NOT NULL,
  `status` VARCHAR(40) NOT NULL DEFAULT 'PENDING',
  `return_code` INT NULL,
  `return_message` VARCHAR(500) NULL,
  `channel` INT NULL,
  `raw_callback` TEXT NULL,
  `paid_at` DATETIME NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`quote_payment_id`),
  KEY `idx_quote_payments_quote` (`quote_request_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `custom_print_payments` (
  `custom_payment_id` BIGINT AUTO_INCREMENT,
  `custom_request_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `provider` VARCHAR(40) NOT NULL DEFAULT 'ZALOPAY',
  `app_trans_id` VARCHAR(64) NOT NULL UNIQUE,
  `zp_trans_token` VARCHAR(255) NULL,
  `zp_trans_id` VARCHAR(100) NULL,
  `order_url` VARCHAR(1000) NULL,
  `amount` DECIMAL(12,2) NOT NULL,
  `status` VARCHAR(40) NOT NULL DEFAULT 'PENDING',
  `return_code` INT NULL,
  `return_message` VARCHAR(500) NULL,
  `channel` INT NULL,
  `raw_callback` TEXT NULL,
  `paid_at` DATETIME NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`custom_payment_id`),
  KEY `idx_custom_print_payments_req` (`custom_request_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
