-- Back up the existing printkraft database before applying this idempotent migration.
USE printkraft;

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
