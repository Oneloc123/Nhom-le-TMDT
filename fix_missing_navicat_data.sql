USE `printkraft`;

DELIMITER $$

DROP PROCEDURE IF EXISTS add_column_if_not_exists$$
CREATE PROCEDURE add_column_if_not_exists(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_definition VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND column_name = p_column
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

CALL add_column_if_not_exists('orders', 'recipient_name_snapshot', 'VARCHAR(120) NOT NULL DEFAULT \'\' AFTER `address_id`');
CALL add_column_if_not_exists('orders', 'recipient_phone_snapshot', 'VARCHAR(25) NOT NULL DEFAULT \'\' AFTER `recipient_name_snapshot`');
CALL add_column_if_not_exists('orders', 'shipping_address_snapshot', 'VARCHAR(500) NOT NULL DEFAULT \'\' AFTER `recipient_phone_snapshot`');

UPDATE `orders` o
LEFT JOIN `addresses` a ON a.address_id = o.address_id
LEFT JOIN `users` u ON u.user_id = o.user_id
SET o.recipient_name_snapshot = COALESCE(NULLIF(a.recipient_name, ''), NULLIF(u.full_name, ''), 'Khách hàng'),
    o.recipient_phone_snapshot = COALESCE(NULLIF(a.recipient_phone, ''), NULLIF(u.phone, ''), '0900000000'),
    o.shipping_address_snapshot = COALESCE(NULLIF(a.address_line, ''), 'Địa chỉ nhận hàng')
WHERE o.recipient_name_snapshot = '' OR o.recipient_name_snapshot IS NULL;

CALL add_column_if_not_exists('payments', 'provider_transaction_no', 'VARCHAR(100) NULL AFTER `provider_transaction_ref`');
CALL add_column_if_not_exists('payments', 'response_code', 'VARCHAR(10) NULL AFTER `provider_transaction_no`');
CALL add_column_if_not_exists('payments', 'bank_code', 'VARCHAR(30) NULL AFTER `response_code`');
CALL add_column_if_not_exists('payments', 'card_type', 'VARCHAR(30) NULL AFTER `bank_code`');

CALL add_column_if_not_exists('reviews', 'created_at', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER `is_published`');

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

CALL add_column_if_not_exists('order_messages', 'sender_role', 'VARCHAR(20) NOT NULL DEFAULT \'CUSTOMER\' AFTER `sender_user_id`');
CALL add_column_if_not_exists('order_messages', 'escalation_id', 'BIGINT DEFAULT NULL AFTER `read_at`');

UPDATE `order_messages` om
SET om.sender_role = 'SHOP'
WHERE om.sender_user_id IN (SELECT owner_user_id FROM print_shops WHERE owner_user_id IS NOT NULL)
  AND om.sender_role = 'CUSTOMER';

DROP PROCEDURE IF EXISTS add_column_if_not_exists;
