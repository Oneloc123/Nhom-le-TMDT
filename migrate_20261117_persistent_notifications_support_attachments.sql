-- Back up the existing printkraft database before applying this idempotent migration.
-- Intended for MariaDB 10.4+ and an existing users table. Run against the intended target.
USE printkraft;

-- UserDao/AuthFilter require these account/security fields in older databases.
ALTER TABLE `users`
 ADD COLUMN IF NOT EXISTS `auth_provider` VARCHAR(40) NOT NULL DEFAULT 'local',
 ADD COLUMN IF NOT EXISTS `provider_user_id` VARCHAR(190) NULL,
 ADD COLUMN IF NOT EXISTS `avatar_url` VARCHAR(800) NULL,
 ADD COLUMN IF NOT EXISTS `birth_date` DATE NULL,
 ADD COLUMN IF NOT EXISTS `gender` VARCHAR(20) NULL,
 ADD COLUMN IF NOT EXISTS `notify_email` TINYINT(1) NOT NULL DEFAULT 1,
 ADD COLUMN IF NOT EXISTS `last_login_at` DATETIME NULL,
 ADD COLUMN IF NOT EXISTS `deleted_at` DATETIME NULL,
 ADD COLUMN IF NOT EXISTS `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 ADD COLUMN IF NOT EXISTS `failed_login_attempts` TINYINT UNSIGNED NOT NULL DEFAULT 0,
 ADD COLUMN IF NOT EXISTS `locked_until` DATETIME NULL;

CREATE TABLE IF NOT EXISTS `user_login_history` (
  `login_history_id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `auth_provider` VARCHAR(40) NOT NULL,
  `login_result` VARCHAR(16) NOT NULL,
  `ip_address` VARCHAR(45) NULL,
  `user_agent` VARCHAR(500) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`login_history_id`),
  KEY `idx_user_login_history_user_created` (`user_id`,`created_at`,`login_history_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_security_events` (
  `security_event_id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `event_type` VARCHAR(60) NOT NULL,
  `description` VARCHAR(500) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`security_event_id`),
  KEY `idx_user_security_events_user_created` (`user_id`,`created_at`,`security_event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create the support schema if it does not exist. Existing tables are retained.
CREATE TABLE IF NOT EXISTS `support_tickets` (
  `ticket_id` BIGINT NOT NULL AUTO_INCREMENT,
  `ticket_code` VARCHAR(40) NOT NULL,
  `user_id` BIGINT NOT NULL,
  `order_id` BIGINT NULL,
  `shop_id` BIGINT NULL,
  `category` VARCHAR(60) NOT NULL,
  `subject` VARCHAR(180) NOT NULL,
  `priority` VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  `status` VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  `assigned_admin_user_id` BIGINT NULL,
  `sla_due_at` DATETIME NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `resolved_at` DATETIME NULL,
  `closed_at` DATETIME NULL,
  `customer_rating` TINYINT UNSIGNED NULL,
  PRIMARY KEY (`ticket_id`),
  UNIQUE KEY `uq_support_ticket_code` (`ticket_code`),
  KEY `idx_support_tickets_user_created` (`user_id`,`created_at`,`ticket_id`),
  KEY `idx_support_tickets_status_priority_sla` (`status`,`priority`,`sla_due_at`),
  KEY `idx_support_tickets_assigned` (`assigned_admin_user_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Ensure both core and Admin fields exist on older versions of support_tickets.
ALTER TABLE `support_tickets`
 ADD COLUMN IF NOT EXISTS `ticket_code` VARCHAR(40) NULL,
 ADD COLUMN IF NOT EXISTS `user_id` BIGINT NULL,
 ADD COLUMN IF NOT EXISTS `order_id` BIGINT NULL,
 ADD COLUMN IF NOT EXISTS `shop_id` BIGINT NULL,
 ADD COLUMN IF NOT EXISTS `category` VARCHAR(60) NULL,
 ADD COLUMN IF NOT EXISTS `subject` VARCHAR(180) NULL,
 ADD COLUMN IF NOT EXISTS `priority` VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
 ADD COLUMN IF NOT EXISTS `status` VARCHAR(32) NOT NULL DEFAULT 'OPEN',
 ADD COLUMN IF NOT EXISTS `assigned_admin_user_id` BIGINT NULL,
 ADD COLUMN IF NOT EXISTS `sla_due_at` DATETIME NULL,
 ADD COLUMN IF NOT EXISTS `created_at` DATETIME NULL,
 ADD COLUMN IF NOT EXISTS `updated_at` DATETIME NULL,
 ADD COLUMN IF NOT EXISTS `resolved_at` DATETIME NULL,
 ADD COLUMN IF NOT EXISTS `closed_at` DATETIME NULL,
 ADD COLUMN IF NOT EXISTS `customer_rating` TINYINT UNSIGNED NULL;

CREATE TABLE IF NOT EXISTS `support_ticket_messages` (
  `ticket_message_id` BIGINT NOT NULL AUTO_INCREMENT,
  `ticket_id` BIGINT NOT NULL,
  `sender_user_id` BIGINT NOT NULL,
  `message_body` TEXT NOT NULL,
  `is_internal` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ticket_message_id`),
  KEY `idx_support_ticket_messages_ticket` (`ticket_id`,`created_at`,`ticket_message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `support_ticket_messages`
 ADD COLUMN IF NOT EXISTS `ticket_id` BIGINT NULL,
 ADD COLUMN IF NOT EXISTS `sender_user_id` BIGINT NULL,
 ADD COLUMN IF NOT EXISTS `message_body` TEXT NULL,
 ADD COLUMN IF NOT EXISTS `is_internal` TINYINT(1) NOT NULL DEFAULT 0,
 ADD COLUMN IF NOT EXISTS `created_at` DATETIME NULL;

-- Persistent notifications: database-backed read state and real event history.
CREATE TABLE IF NOT EXISTS `notifications` (
 `notification_id` BIGINT NOT NULL AUTO_INCREMENT, `user_id` BIGINT NOT NULL,
 `notification_type` VARCHAR(40) NOT NULL, `title` VARCHAR(180) NOT NULL,
 `message` VARCHAR(1000) NOT NULL, `target_url` VARCHAR(800) NULL,
 `related_type` VARCHAR(40) NULL, `related_id` BIGINT NULL,
 `is_read` TINYINT(1) NOT NULL DEFAULT 0, `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY (`notification_id`), KEY `idx_notifications_user_created` (`user_id`,`created_at`,`notification_id`),
 KEY `idx_notifications_user_read` (`user_id`,`is_read`,`created_at`), KEY `idx_notifications_related` (`related_type`,`related_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- Private complaint evidence; downloads are served by a permission-checked servlet.
CREATE TABLE IF NOT EXISTS `support_ticket_attachments` (
 `attachment_id` BIGINT NOT NULL AUTO_INCREMENT, `ticket_id` BIGINT NOT NULL,
 `uploaded_by_user_id` BIGINT NOT NULL, `original_filename` VARCHAR(255) NOT NULL,
 `content_type` VARCHAR(100) NOT NULL, `file_size` BIGINT NOT NULL, `file_data` MEDIUMBLOB NOT NULL,
 `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (`attachment_id`),
 KEY `idx_support_attachments_ticket` (`ticket_id`,`created_at`,`attachment_id`),
 KEY `idx_support_attachments_uploader` (`uploaded_by_user_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
