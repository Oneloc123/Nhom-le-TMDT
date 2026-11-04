-- Migration tương thích cho file dump printkraft.sql ngày 04/11/2026 của nhóm,
-- luồng đặt hàng mới và tích hợp VNPAY. File này không thay thế toàn bộ schema
-- tài khoản mới; hãy import dump của nhóm trước nếu database vẫn dùng schema cũ.
--
-- Chạy sau khi đã chọn đúng database. Script có thể chạy lặp lại.

DELIMITER $$

DROP PROCEDURE IF EXISTS add_printkraft_column_if_missing$$
CREATE PROCEDURE add_printkraft_column_if_missing(
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

-- File dump của nhóm có tracking/toạ độ nhưng thiếu ba snapshot mà OrderDao sử dụng.
CALL add_printkraft_column_if_missing(
    'orders', 'recipient_name_snapshot',
    'VARCHAR(120) NULL AFTER `address_id`'
)$$
CALL add_printkraft_column_if_missing(
    'orders', 'recipient_phone_snapshot',
    'VARCHAR(25) NULL AFTER `recipient_name_snapshot`'
)$$
CALL add_printkraft_column_if_missing(
    'orders', 'shipping_address_snapshot',
    'VARCHAR(500) NULL AFTER `recipient_phone_snapshot`'
)$$

-- Schema cũ chưa có các trường mới của luồng giao hàng/theo dõi.
CALL add_printkraft_column_if_missing(
    'orders', 'shipping_latitude',
    'DECIMAL(10,7) NULL AFTER `cancelled_at`'
)$$
CALL add_printkraft_column_if_missing(
    'orders', 'shipping_longitude',
    'DECIMAL(10,7) NULL AFTER `shipping_latitude`'
)$$
CALL add_printkraft_column_if_missing(
    'orders', 'delivery_note_snapshot',
    'VARCHAR(500) NULL AFTER `shipping_longitude`'
)$$
CALL add_printkraft_column_if_missing(
    'orders', 'customer_note',
    'VARCHAR(1000) NULL AFTER `delivery_note_snapshot`'
)$$
CALL add_printkraft_column_if_missing(
    'orders', 'cancel_reason',
    'VARCHAR(500) NULL AFTER `customer_note`'
)$$
CALL add_printkraft_column_if_missing(
    'orders', 'tracking_token',
    'CHAR(64) NULL AFTER `cancel_reason`'
)$$
CALL add_printkraft_column_if_missing(
    'orders', 'tracking_enabled',
    'TINYINT(1) NOT NULL DEFAULT 0 AFTER `tracking_token`'
)$$
CALL add_printkraft_column_if_missing(
    'orders', 'estimated_delivery_at',
    'DATETIME NULL AFTER `tracking_enabled`'
)$$

-- Các trường đối soát VNPAY chỉ được ghi từ callback/IPN đã xác minh.
CALL add_printkraft_column_if_missing(
    'payments', 'provider_transaction_no',
    'VARCHAR(100) NULL AFTER `provider_transaction_ref`'
)$$
CALL add_printkraft_column_if_missing(
    'payments', 'response_code',
    'VARCHAR(10) NULL AFTER `provider_transaction_no`'
)$$
CALL add_printkraft_column_if_missing(
    'payments', 'bank_code',
    'VARCHAR(30) NULL AFTER `response_code`'
)$$
CALL add_printkraft_column_if_missing(
    'payments', 'card_type',
    'VARCHAR(30) NULL AFTER `bank_code`'
)$$

DROP PROCEDURE add_printkraft_column_if_missing$$

DELIMITER ;

-- Điền snapshot cho dữ liệu cũ trong dump. Đơn mới luôn được OrderDao ghi trực tiếp.
UPDATE orders o
LEFT JOIN addresses a ON a.address_id = o.address_id
LEFT JOIN users u ON u.user_id = o.user_id
SET o.recipient_name_snapshot = COALESCE(
        NULLIF(o.recipient_name_snapshot, ''),
        NULLIF(a.recipient_name, ''),
        NULLIF(u.full_name, ''),
        'Khách hàng'
    ),
    o.recipient_phone_snapshot = COALESCE(
        NULLIF(o.recipient_phone_snapshot, ''),
        NULLIF(a.recipient_phone, ''),
        NULLIF(u.phone, ''),
        ''
    ),
    o.shipping_address_snapshot = COALESCE(
        NULLIF(o.shipping_address_snapshot, ''),
        NULLIF(CONCAT_WS(', ',
            NULLIF(a.address_line, ''),
            NULLIF(a.district, ''),
            NULLIF(a.province, '')
        ), ''),
        'Địa chỉ chưa cập nhật'
    )
WHERE o.recipient_name_snapshot IS NULL
   OR o.recipient_name_snapshot = ''
   OR o.recipient_phone_snapshot IS NULL
   OR o.shipping_address_snapshot IS NULL
   OR o.shipping_address_snapshot = '';
