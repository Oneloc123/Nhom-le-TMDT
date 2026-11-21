-- PrintKraft Admin operations and integration-ready schema additions.
-- IMPORTANT: back up first. Run after selecting the intended existing printkraft schema.
-- This is additive and never rewrites historical order, shipment, payment, ticket, or seller data.

SELECT VERSION() AS server_version, DATABASE() AS selected_database;

CREATE TABLE IF NOT EXISTS `shipment_events` (
  `shipment_event_id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `shipment_id` BIGINT NULL,
  `event_title` VARCHAR(180) NOT NULL,
  `description` VARCHAR(1000) NULL,
  `occurred_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`shipment_event_id`),
  KEY `idx_shipment_events_order_time` (`order_id`,`occurred_at`,`shipment_event_id`),
  KEY `idx_shipment_events_shipment_time` (`shipment_id`,`occurred_at`,`shipment_event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Older installations may already have the shipment timeline table without its Admin linkage column.
ALTER TABLE `shipment_events`
  ADD COLUMN IF NOT EXISTS `shipment_id` BIGINT NULL,
  ADD INDEX IF NOT EXISTS `idx_shipment_events_shipment_time` (`shipment_id`,`occurred_at`,`shipment_event_id`);

CREATE TABLE IF NOT EXISTS `seller_verification_events` (
  `event_id` BIGINT NOT NULL AUTO_INCREMENT,
  `shop_id` BIGINT NOT NULL,
  `admin_user_id` BIGINT NOT NULL,
  `action` VARCHAR(24) NOT NULL,
  `from_status` VARCHAR(24) NOT NULL,
  `to_status` VARCHAR(24) NOT NULL,
  `reason` VARCHAR(1000) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`event_id`),
  KEY `idx_seller_verification_shop_time` (`shop_id`,`created_at`,`event_id`),
  KEY `idx_seller_verification_admin_time` (`admin_user_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `admin_manual_refunds` (
  `refund_id` BIGINT NOT NULL AUTO_INCREMENT,
  `payment_id` BIGINT NOT NULL,
  `order_id` BIGINT NOT NULL,
  `admin_user_id` BIGINT NOT NULL,
  `amount` DECIMAL(12,2) NOT NULL,
  `method` VARCHAR(40) NOT NULL,
  `reference` VARCHAR(120) NULL,
  `note` VARCHAR(1000) NOT NULL,
  `status` VARCHAR(16) NOT NULL,
  `recorded_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `success_payment_id` BIGINT GENERATED ALWAYS AS
    (CASE WHEN `status`='SUCCESS' THEN `payment_id` ELSE NULL END) PERSISTENT,
  PRIMARY KEY (`refund_id`),
  UNIQUE KEY `uq_admin_manual_refund_payment_success` (`success_payment_id`),
  KEY `idx_admin_manual_refund_payment_status` (`payment_id`,`status`),
  UNIQUE KEY `uq_admin_manual_refund_reference` (`method`,`reference`),
  KEY `idx_admin_manual_refund_order_time` (`order_id`,`recorded_at`),
  KEY `idx_admin_manual_refund_admin_time` (`admin_user_id`,`recorded_at`),
  CONSTRAINT `chk_admin_manual_refund_amount` CHECK (`amount` > 0),
  CONSTRAINT `chk_admin_manual_refund_status` CHECK (`status` IN ('SUCCESS','FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `support_ticket_events` (
  `event_id` BIGINT NOT NULL AUTO_INCREMENT,
  `ticket_id` BIGINT NOT NULL,
  `admin_user_id` BIGINT NOT NULL,
  `old_status` VARCHAR(32) NOT NULL,
  `new_status` VARCHAR(32) NOT NULL,
  `old_priority` VARCHAR(20) NOT NULL,
  `new_priority` VARCHAR(20) NOT NULL,
  `old_assigned_admin_user_id` BIGINT NULL,
  `new_assigned_admin_user_id` BIGINT NULL,
  `note` VARCHAR(1000) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`event_id`),
  KEY `idx_support_ticket_events_ticket_time` (`ticket_id`,`created_at`,`event_id`),
  KEY `idx_support_ticket_events_admin_time` (`admin_user_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `support_tickets`
  ADD COLUMN IF NOT EXISTS `sla_remaining_seconds` INT UNSIGNED NULL,
  ADD COLUMN IF NOT EXISTS `sla_paused_at` DATETIME NULL,
  ADD COLUMN IF NOT EXISTS `first_response_at` DATETIME NULL;

-- Keep a historical verification note on rejection/request-info; status remains PENDING for REQUEST_INFO.
-- See verify_admin_schema.sql for postflight checks.
SELECT TABLE_NAME
FROM information_schema.TABLES
WHERE TABLE_SCHEMA=DATABASE()
  AND TABLE_NAME IN ('shipment_events','seller_verification_events','admin_manual_refunds','support_ticket_events')
ORDER BY TABLE_NAME;

SELECT TABLE_NAME,COLUMN_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='support_tickets'
  AND COLUMN_NAME IN ('sla_remaining_seconds','sla_paused_at','first_response_at')
ORDER BY COLUMN_NAME;
