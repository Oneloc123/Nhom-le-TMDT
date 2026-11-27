-- PrintKraft Phase 2: carrier tracking, webhook audit, reminders and 72-hour receipt automation.
-- MariaDB 10.4 compatible. Back up the database before applying.
-- Run after migrate_delivery_confirmation_phase1.sql.

ALTER TABLE shipments
  ADD COLUMN IF NOT EXISTS carrier_provider VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
  ADD COLUMN IF NOT EXISTS carrier_status_code VARCHAR(50) NULL,
  ADD COLUMN IF NOT EXISTS carrier_status_updated_at DATETIME NULL,
  ADD COLUMN IF NOT EXISTS last_carrier_sync_at DATETIME NULL,
  ADD COLUMN IF NOT EXISTS carrier_delivery_verified_at DATETIME NULL,
  ADD COLUMN IF NOT EXISTS carrier_pod_url VARCHAR(1000) NULL,
  ADD COLUMN IF NOT EXISTS receipt_deadline_at DATETIME NULL,
  ADD COLUMN IF NOT EXISTS receipt_reminder_sent_at DATETIME NULL,
  ADD COLUMN IF NOT EXISTS receipt_auto_confirmed_at DATETIME NULL,
  ADD INDEX IF NOT EXISTS idx_shipments_carrier_poll
    (carrier_provider, status, last_carrier_sync_at),
  ADD INDEX IF NOT EXISTS idx_shipments_receipt_deadline
    (receipt_status, receipt_deadline_at);

-- Existing records are manual by default. Explicit GHN names are opt-in for polling.
UPDATE shipments
SET carrier_provider='GHN'
WHERE UPPER(TRIM(COALESCE(carrier,''))) IN ('GHN','GIAO HÀNG NHANH','GIAO HANG NHANH')
  AND carrier_provider='MANUAL';

CREATE TABLE IF NOT EXISTS carrier_webhook_events (
  event_id BIGINT NOT NULL AUTO_INCREMENT,
  provider VARCHAR(20) NOT NULL,
  event_key CHAR(64) NOT NULL,
  provider_order_code VARCHAR(120) NOT NULL,
  event_type VARCHAR(40) NOT NULL,
  external_status VARCHAR(60) NULL,
  event_time DATETIME NULL,
  shipment_id BIGINT NULL,
  processing_status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
  error_message VARCHAR(500) NULL,
  payload_json LONGTEXT NOT NULL,
  received_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  processed_at DATETIME NULL,
  PRIMARY KEY (event_id),
  UNIQUE KEY uq_carrier_webhook_event (provider, event_key),
  KEY idx_carrier_webhook_order (provider, provider_order_code, received_at),
  KEY idx_carrier_webhook_shipment (shipment_id, received_at),
  KEY idx_carrier_webhook_status (processing_status, received_at),
  CONSTRAINT fk_carrier_webhook_shipment FOREIGN KEY (shipment_id)
    REFERENCES shipments(shipment_id) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- New GHN deliveries start their customer-confirmation deadline from the carrier's
-- authenticated delivered event. Manual/historical deliveries are deliberately excluded.
SELECT TABLE_NAME, COLUMN_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA=DATABASE()
  AND TABLE_NAME='shipments'
  AND COLUMN_NAME IN ('carrier_provider','carrier_status_code','carrier_delivery_verified_at',
                      'receipt_deadline_at','receipt_reminder_sent_at','receipt_auto_confirmed_at');

SELECT TABLE_NAME
FROM information_schema.TABLES
WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='carrier_webhook_events';
