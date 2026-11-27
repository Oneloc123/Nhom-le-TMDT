USE `printkraft`;

CREATE TABLE IF NOT EXISTS `order_message_escalations` (
    `escalation_id`     BIGINT AUTO_INCREMENT,
    `order_id`          BIGINT NOT NULL,
    `shop_id`           BIGINT NOT NULL,
    `requested_by`      BIGINT NOT NULL,
    `requester_role`    VARCHAR(20) NOT NULL,
    `reason`            TEXT NOT NULL,
    `status`            VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    `assigned_admin_id` BIGINT DEFAULT NULL,
    `verdict`           TEXT DEFAULT NULL,
    `created_at`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `resolved_at`       DATETIME DEFAULT NULL,
    PRIMARY KEY (`escalation_id`),
    INDEX `idx_esc_order_shop` (`order_id`, `shop_id`),
    INDEX `idx_esc_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DELIMITER $$

DROP PROCEDURE IF EXISTS add_order_message_escalation_cols$$
CREATE PROCEDURE add_order_message_escalation_cols()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE table_schema = DATABASE() AND table_name = 'order_messages' AND column_name = 'sender_role'
    ) THEN
        ALTER TABLE `order_messages` ADD COLUMN `sender_role` VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER' AFTER `sender_user_id`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE table_schema = DATABASE() AND table_name = 'order_messages' AND column_name = 'escalation_id'
    ) THEN
        ALTER TABLE `order_messages` ADD COLUMN `escalation_id` BIGINT DEFAULT NULL AFTER `read_at`;
    END IF;
END$$

DELIMITER ;

CALL add_order_message_escalation_cols();
DROP PROCEDURE IF EXISTS add_order_message_escalation_cols;

UPDATE order_messages om
SET om.sender_role = 'SHOP'
WHERE om.sender_user_id IN (SELECT owner_user_id FROM print_shops WHERE owner_user_id IS NOT NULL)
  AND om.sender_role = 'CUSTOMER';
