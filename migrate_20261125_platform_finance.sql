-- PrintKraft financial core migration
-- Target: MySQL 8 / MariaDB 10.4+, InnoDB, utf8mb4.
-- Safe additive migration: does not alter or delete existing orders/payments/items.
-- Run only after backup and after selecting the intended printkraft database.

CREATE TABLE IF NOT EXISTS platform_finance_settings (
    setting_id TINYINT UNSIGNED NOT NULL,
    commission_rate DECIMAL(5,2) NOT NULL DEFAULT 10.00,
    commission_basis VARCHAR(30) NOT NULL DEFAULT 'PRODUCT_SUBTOTAL',
    settlement_cycle VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    cod_collection_mode VARCHAR(30) NOT NULL DEFAULT 'PLATFORM',
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    updated_by_user_id BIGINT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (setting_id),
    CONSTRAINT chk_platform_finance_settings_singleton CHECK (setting_id = 1),
    CONSTRAINT chk_platform_finance_commission CHECK (commission_rate >= 0 AND commission_rate <= 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO platform_finance_settings
    (setting_id, commission_rate, commission_basis, settlement_cycle, cod_collection_mode, currency)
VALUES (1, 10.00, 'PRODUCT_SUBTOTAL', 'MONTHLY', 'PLATFORM', 'VND')
ON DUPLICATE KEY UPDATE commission_rate=10.00, commission_basis='PRODUCT_SUBTOTAL',
    settlement_cycle='MONTHLY', cod_collection_mode='PLATFORM', currency='VND';

CREATE TABLE IF NOT EXISTS order_financial_allocations (
    allocation_id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    shop_id BIGINT NOT NULL,
    payment_id BIGINT NULL,
    settlement_id BIGINT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    gross_product_amount DECIMAL(12,2) NOT NULL,
    commission_rate_snapshot DECIMAL(5,2) NOT NULL,
    commission_amount DECIMAL(12,2) NOT NULL,
    seller_net_amount DECIMAL(12,2) NOT NULL,
    allocation_status VARCHAR(24) NOT NULL DEFAULT 'AVAILABLE',
    settled_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (allocation_id),
    UNIQUE KEY uq_financial_allocation_order_item (order_item_id),
    KEY idx_financial_allocation_shop_status (shop_id, allocation_status, created_at),
    KEY idx_financial_allocation_order (order_id),
    KEY idx_financial_allocation_payment (payment_id),
    CONSTRAINT fk_financial_allocation_order FOREIGN KEY (order_id) REFERENCES orders(order_id),
    CONSTRAINT fk_financial_allocation_item FOREIGN KEY (order_item_id) REFERENCES order_items(order_item_id),
    CONSTRAINT fk_financial_allocation_shop FOREIGN KEY (shop_id) REFERENCES print_shops(shop_id),
    CONSTRAINT fk_financial_allocation_payment FOREIGN KEY (payment_id) REFERENCES payments(payment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS financial_cod_receipts (
    cod_receipt_id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    payment_id BIGINT NOT NULL,
    collected_amount DECIMAL(12,2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    carrier_name VARCHAR(120) NOT NULL,
    carrier_reference VARCHAR(120) NOT NULL,
    received_by_user_id BIGINT NOT NULL,
    received_at DATETIME NOT NULL,
    note VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (cod_receipt_id),
    UNIQUE KEY uq_financial_cod_receipt_order (order_id),
    UNIQUE KEY uq_financial_cod_receipt_reference (carrier_reference),
    CONSTRAINT fk_financial_cod_order FOREIGN KEY (order_id) REFERENCES orders(order_id),
    CONSTRAINT fk_financial_cod_payment FOREIGN KEY (payment_id) REFERENCES payments(payment_id),
    CONSTRAINT fk_financial_cod_actor FOREIGN KEY (received_by_user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS seller_monthly_settlements (
    settlement_id BIGINT NOT NULL AUTO_INCREMENT,
    shop_id BIGINT NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    gross_product_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    commission_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    seller_net_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    refund_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    commission_reversal_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    seller_recovery_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'READY',
    payout_reference VARCHAR(120) NULL,
    paid_by_user_id BIGINT NULL,
    paid_at DATETIME NULL,
    note VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (settlement_id),
    UNIQUE KEY uq_seller_monthly_settlement (shop_id, period_start, period_end, currency),
    UNIQUE KEY uq_seller_monthly_settlement_payout_reference (payout_reference),
    KEY idx_seller_monthly_settlement_status (status, period_start),
    CONSTRAINT fk_seller_monthly_settlement_shop FOREIGN KEY (shop_id) REFERENCES print_shops(shop_id),
    CONSTRAINT fk_seller_monthly_settlement_actor FOREIGN KEY (paid_by_user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS financial_ledger (
    ledger_id BIGINT NOT NULL AUTO_INCREMENT,
    allocation_id BIGINT NULL,
    settlement_id BIGINT NULL,
    order_id BIGINT NULL,
    shop_id BIGINT NULL,
    entry_type VARCHAR(32) NOT NULL,
    debit_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    credit_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    reference_code VARCHAR(120) NULL,
    description VARCHAR(500) NULL,
    created_by_user_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (ledger_id),
    KEY idx_financial_ledger_order (order_id, created_at),
    KEY idx_financial_ledger_shop (shop_id, created_at),
    KEY idx_financial_ledger_settlement (settlement_id),
    CONSTRAINT fk_financial_ledger_allocation FOREIGN KEY (allocation_id) REFERENCES order_financial_allocations(allocation_id),
    CONSTRAINT fk_financial_ledger_settlement FOREIGN KEY (settlement_id) REFERENCES seller_monthly_settlements(settlement_id),
    CONSTRAINT fk_financial_ledger_order FOREIGN KEY (order_id) REFERENCES orders(order_id),
    CONSTRAINT fk_financial_ledger_shop FOREIGN KEY (shop_id) REFERENCES print_shops(shop_id),
    CONSTRAINT fk_financial_ledger_actor FOREIGN KEY (created_by_user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS financial_refund_adjustments (
    adjustment_id BIGINT NOT NULL AUTO_INCREMENT,
    allocation_id BIGINT NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    source_id BIGINT NOT NULL,
    refund_amount DECIMAL(12,2) NOT NULL,
    commission_reversal DECIMAL(12,2) NOT NULL,
    seller_recovery DECIMAL(12,2) NOT NULL,
    adjustment_status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    settlement_id BIGINT NULL,
    created_by_user_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (adjustment_id),
    UNIQUE KEY uq_financial_refund_source_allocation (source_type, source_id, allocation_id),
    KEY idx_financial_refund_adjustment_status (adjustment_status, created_at),
    KEY idx_financial_refund_adjustment_settlement (settlement_id),
    CONSTRAINT fk_financial_refund_allocation FOREIGN KEY (allocation_id) REFERENCES order_financial_allocations(allocation_id),
    CONSTRAINT fk_financial_refund_settlement FOREIGN KEY (settlement_id) REFERENCES seller_monthly_settlements(settlement_id),
    CONSTRAINT fk_financial_refund_actor FOREIGN KEY (created_by_user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Upgrade an installation that may already have run an earlier revision of this migration.
DELIMITER //
DROP PROCEDURE IF EXISTS add_pk_finance_column_if_missing//
CREATE PROCEDURE add_pk_finance_column_if_missing(
    IN table_name_value VARCHAR(64),
    IN column_name_value VARCHAR(64),
    IN definition_value VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema=DATABASE()
          AND table_name=table_name_value
          AND column_name=column_name_value
    ) THEN
        SET @finance_alter = CONCAT(
            'ALTER TABLE `', REPLACE(table_name_value, '`', '``'),
            '` ADD COLUMN `', REPLACE(column_name_value, '`', '``'),
            '` ', definition_value
        );
        PREPARE finance_migration_stmt FROM @finance_alter;
        EXECUTE finance_migration_stmt;
        DEALLOCATE PREPARE finance_migration_stmt;
    END IF;
END//
CALL add_pk_finance_column_if_missing('order_financial_allocations','settlement_id',
    'BIGINT NULL AFTER payment_id')//
CALL add_pk_finance_column_if_missing('financial_refund_adjustments','settlement_id',
    'BIGINT NULL AFTER adjustment_status')//
CALL add_pk_finance_column_if_missing('seller_monthly_settlements','refund_amount',
    'DECIMAL(14,2) NOT NULL DEFAULT 0 AFTER seller_net_amount')//
CALL add_pk_finance_column_if_missing('seller_monthly_settlements','commission_reversal_amount',
    'DECIMAL(14,2) NOT NULL DEFAULT 0 AFTER refund_amount')//
CALL add_pk_finance_column_if_missing('seller_monthly_settlements','seller_recovery_amount',
    'DECIMAL(14,2) NOT NULL DEFAULT 0 AFTER commission_reversal_amount')//
DROP PROCEDURE add_pk_finance_column_if_missing//
DELIMITER ;

-- Preflight after migration:
-- SELECT * FROM platform_finance_settings WHERE setting_id = 1;
-- SHOW TABLES LIKE '%financial%';
-- SELECT COUNT(*) AS legacy_orders FROM orders;
-- Existing orders intentionally receive no allocation automatically. They require
-- payment/COD evidence and an explicit audited backfill policy before recognition.
