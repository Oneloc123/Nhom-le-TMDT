-- Admin operational queues: request review audit and payment reconciliation log.
-- Backup the intended printkraft database before running this idempotent migration.
-- The migration never changes order/payment statuses as part of reconciliation.

USE printkraft;

-- Order-change workflow is used by OrderChangeRequestDao. Do not overwrite existing requests.
CREATE TABLE IF NOT EXISTS \`order_change_requests\` (
  \`change_request_id\` BIGINT NOT NULL AUTO_INCREMENT,
  \`order_id\` BIGINT NOT NULL,
  \`order_item_id\` BIGINT NULL,
  \`customer_user_id\` BIGINT NOT NULL,
  \`shop_id\` BIGINT NULL,
  \`request_type\` VARCHAR(40) NOT NULL,
  \`reason\` VARCHAR(1000) NOT NULL,
  \`payload_json\` LONGTEXT NULL,
  \`status\` VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  \`shop_response\` VARCHAR(1000) NULL,
  \`price_delta\` DECIMAL(12,2) NOT NULL DEFAULT 0,
  \`cancel_fee\` DECIMAL(12,2) NOT NULL DEFAULT 0,
  \`customer_confirmed_at\` DATETIME NULL,
  \`expires_at\` DATETIME NULL,
  \`created_at\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  \`updated_at\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (\`change_request_id\`),
  KEY \`idx_order_change_queue\` (\`status\`,\`created_at\`,\`change_request_id\`),
  KEY \`idx_order_change_order_item_type_status\` (\`order_id\`,\`order_item_id\`,\`request_type\`,\`status\`),
  KEY \`idx_order_change_customer_created\` (\`customer_user_id\`,\`created_at\`),
  KEY \`idx_order_change_shop_status\` (\`shop_id\`,\`status\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS \`admin_custom_print_review_events\` (
  \`event_id\` BIGINT NOT NULL AUTO_INCREMENT,
  \`custom_request_id\` BIGINT NOT NULL,
  \`admin_user_id\` BIGINT NOT NULL,
  \`action\` VARCHAR(24) NOT NULL,
  \`note\` VARCHAR(1000) NOT NULL,
  \`created_at\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (\`event_id\`),
  KEY \`idx_admin_custom_print_events_request\` (\`custom_request_id\`,\`event_id\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS \`admin_order_change_review_events\` (
  \`event_id\` BIGINT NOT NULL AUTO_INCREMENT,
  \`change_request_id\` BIGINT NOT NULL,
  \`admin_user_id\` BIGINT NOT NULL,
  \`action\` VARCHAR(24) NOT NULL,
  \`note\` VARCHAR(1000) NOT NULL,
  \`created_at\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (\`event_id\`),
  KEY \`idx_admin_order_change_events_request\` (\`change_request_id\`,\`event_id\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS \`admin_review_moderation_events\` (
  \`event_id\` BIGINT NOT NULL AUTO_INCREMENT,
  \`review_id\` BIGINT NOT NULL,
  \`admin_user_id\` BIGINT NOT NULL,
  \`action\` VARCHAR(24) NOT NULL,
  \`reason\` VARCHAR(1000) NULL,
  \`created_at\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (\`event_id\`),
  KEY \`idx_admin_review_events_review\` (\`review_id\`,\`event_id\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS \`admin_payment_reconciliation_events\` (
  \`event_id\` BIGINT NOT NULL AUTO_INCREMENT,
  \`payment_id\` BIGINT NOT NULL,
  \`admin_user_id\` BIGINT NOT NULL,
  \`outcome\` VARCHAR(24) NOT NULL,
  \`note\` VARCHAR(1000) NOT NULL,
  \`provider_reference\` VARCHAR(120) NULL,
  \`created_at\` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (\`event_id\`),
  KEY \`idx_admin_payment_reconcile_payment\` (\`payment_id\`,\`event_id\`),
  KEY \`idx_admin_payment_reconcile_outcome\` (\`outcome\`,\`created_at\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Postflight: each table should exist in the selected printkraft schema.
SELECT table_name, table_rows
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN (
    'order_change_requests',
    'admin_custom_print_review_events',
    'admin_order_change_review_events',
    'admin_review_moderation_events',
    'admin_payment_reconciliation_events'
  )
ORDER BY table_name;

-- Notes:
-- * Admin review of custom-print requests is an audit/event workflow; quoting/pricing remains
--   with the selected workshop. This migration does not alter custom_print_requests.
-- * Admin rejection of an order-change request is restricted by the DAO to PENDING state.
-- * Payment reconciliation records an operator result and reference only. It never marks money
--   as paid/refunded, never changes orders.payment_status and never issues a bank/gateway call.
