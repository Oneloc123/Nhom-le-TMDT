-- Chạy một lần trên database PrintKraft hiện có trước khi bật VNPAY.
-- Script có thể chạy lại: mỗi cột chỉ được thêm nếu chưa tồn tại.

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

CALL add_printkraft_column_if_missing('payments', 'provider_transaction_no', 'VARCHAR(100) NULL AFTER `provider_transaction_ref`')$$
CALL add_printkraft_column_if_missing('payments', 'response_code', 'VARCHAR(10) NULL AFTER `provider_transaction_no`')$$
CALL add_printkraft_column_if_missing('payments', 'bank_code', 'VARCHAR(30) NULL AFTER `response_code`')$$
CALL add_printkraft_column_if_missing('payments', 'card_type', 'VARCHAR(30) NULL AFTER `bank_code`')$$

DROP PROCEDURE add_printkraft_column_if_missing$$

DELIMITER ;
