-- PrintKraft Phase 1: customer receipt confirmation and delivery disputes.
-- MariaDB 10.4 compatible, additive migration. Back up the target database first.
-- Run against the existing PrintKraft schema; this does not rewrite order/payment data.

ALTER TABLE shipments
  ADD COLUMN IF NOT EXISTS receipt_status VARCHAR(24) NOT NULL DEFAULT 'NOT_READY',
  ADD COLUMN IF NOT EXISTS receipt_confirmed_at DATETIME NULL,
  ADD COLUMN IF NOT EXISTS receipt_confirmed_by_user_id BIGINT NULL,
  ADD INDEX IF NOT EXISTS idx_shipments_receipt_status (order_id, receipt_status);

-- Historical deliveries predate customer confirmation; do not force old orders to
-- confirm again. Only new transitions into DELIVERED create WAITING confirmations.
UPDATE shipments
SET receipt_status='NOT_REQUIRED'
WHERE receipt_status='NOT_READY'
  AND UPPER(TRIM(COALESCE(status,''))) IN ('DELIVERED','COMPLETED','ĐÃ GIAO');

CREATE TABLE IF NOT EXISTS delivery_disputes (
  dispute_id BIGINT NOT NULL AUTO_INCREMENT,
  dispute_code VARCHAR(48) NOT NULL,
  shipment_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  customer_id BIGINT NOT NULL,
  dispute_type VARCHAR(32) NOT NULL,
  description VARCHAR(2000) NOT NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'OPEN',
  resolution_note VARCHAR(2000) NULL,
  admin_id BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  resolved_at DATETIME NULL,
  PRIMARY KEY (dispute_id),
  UNIQUE KEY uq_delivery_dispute_code (dispute_code),
  KEY idx_delivery_dispute_queue (status, created_at, dispute_id),
  KEY idx_delivery_dispute_order (order_id, status),
  KEY idx_delivery_dispute_shipment (shipment_id, status),
  CONSTRAINT fk_delivery_dispute_shipment FOREIGN KEY (shipment_id)
    REFERENCES shipments(shipment_id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_delivery_dispute_order FOREIGN KEY (order_id)
    REFERENCES orders(order_id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_delivery_dispute_customer FOREIGN KEY (customer_id)
    REFERENCES users(user_id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_delivery_dispute_admin FOREIGN KEY (admin_id)
    REFERENCES users(user_id) ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT chk_delivery_dispute_type CHECK
    (dispute_type IN ('NOT_RECEIVED','DAMAGED','MISSING','WRONG_ITEM','OTHER')),
  CONSTRAINT chk_delivery_dispute_status CHECK
    (status IN ('OPEN','UNDER_REVIEW','RESOLVED','REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS delivery_dispute_events (
  event_id BIGINT NOT NULL AUTO_INCREMENT,
  dispute_id BIGINT NOT NULL,
  actor_user_id BIGINT NULL,
  event_action VARCHAR(32) NOT NULL,
  from_status VARCHAR(24) NULL,
  to_status VARCHAR(24) NOT NULL,
  note VARCHAR(2000) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (event_id),
  KEY idx_delivery_dispute_events_timeline (dispute_id, created_at, event_id),
  CONSTRAINT fk_delivery_dispute_event_dispute FOREIGN KEY (dispute_id)
    REFERENCES delivery_disputes(dispute_id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_delivery_dispute_event_actor FOREIGN KEY (actor_user_id)
    REFERENCES users(user_id) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Optional FK for the receipt confirmer. Keep nullable for legacy rows.
-- Verify the actual user key type and existing constraints before adding this FK.
SELECT TABLE_NAME, COLUMN_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA=DATABASE()
  AND TABLE_NAME IN ('shipments','delivery_disputes','delivery_dispute_events')
ORDER BY TABLE_NAME, ORDINAL_POSITION;

SELECT TABLE_NAME
FROM information_schema.TABLES
WHERE TABLE_SCHEMA=DATABASE()
  AND TABLE_NAME IN ('delivery_disputes','delivery_dispute_events')
ORDER BY TABLE_NAME;
