-- PrintKraft: customer returns/refunds workflow migration.
-- Apply to a backed-up MariaDB schema that already contains orders, order_items,
-- users, shipments, and the support_tickets/support_ticket_messages tables used by SupportDao.
-- This migration adds new tables/columns only; it does not drop or rewrite
-- existing order/payment records. Review SHOW CREATE TABLE output before production use.

CREATE TABLE IF NOT EXISTS return_refund_requests (
  request_id BIGINT NOT NULL AUTO_INCREMENT,
  request_code VARCHAR(40) NOT NULL,
  user_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  order_item_id BIGINT NOT NULL,
  support_ticket_id BIGINT NULL,
  request_type VARCHAR(30) NOT NULL,
  reason_code VARCHAR(40) NOT NULL,
  requested_quantity INT NOT NULL,
  requested_amount DECIMAL(12,2) NOT NULL,
  approved_amount DECIMAL(12,2) NULL,
  currency CHAR(3) NOT NULL DEFAULT 'VND',
  status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED',
  customer_note VARCHAR(2000) NOT NULL,
  decision_note VARCHAR(2000) NULL,
  policy_version VARCHAR(40) NOT NULL,
  return_deadline_at DATETIME NOT NULL,
  return_carrier VARCHAR(100) NULL,
  return_tracking_number VARCHAR(120) NULL,
  return_shipped_at DATETIME NULL,
  resolution_method VARCHAR(40) NULL,
  refund_reference VARCHAR(120) NULL,
  decided_by_user_id BIGINT NULL,
  submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  decided_at DATETIME NULL,
  resolved_at DATETIME NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (request_id),
  UNIQUE KEY uq_return_refund_request_code (request_code),
  KEY idx_return_refund_customer (user_id, submitted_at),
  KEY idx_return_refund_order (order_id, order_item_id, status),
  KEY idx_return_refund_review_queue (status, submitted_at),
  KEY idx_return_refund_decider (decided_by_user_id),
  CONSTRAINT fk_return_refund_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_return_refund_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_return_refund_order_item FOREIGN KEY (order_item_id) REFERENCES order_items(order_item_id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_return_refund_decider FOREIGN KEY (decided_by_user_id) REFERENCES users(user_id) ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT chk_return_refund_quantity CHECK (requested_quantity > 0),
  CONSTRAINT chk_return_refund_amount CHECK (requested_amount > 0 AND (approved_amount IS NULL OR approved_amount > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS return_refund_evidence (
  evidence_id BIGINT NOT NULL AUTO_INCREMENT,
  request_id BIGINT NOT NULL,
  uploaded_by_user_id BIGINT NOT NULL,
  original_filename VARCHAR(180) NOT NULL,
  storage_key VARCHAR(240) NOT NULL,
  content_type VARCHAR(80) NOT NULL,
  file_size BIGINT NOT NULL,
  sha256 CHAR(64) NOT NULL,
  uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (evidence_id),
  KEY idx_return_refund_evidence_request (request_id, uploaded_at),
  CONSTRAINT fk_return_refund_evidence_request FOREIGN KEY (request_id) REFERENCES return_refund_requests(request_id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_return_refund_evidence_user FOREIGN KEY (uploaded_by_user_id) REFERENCES users(user_id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT chk_return_refund_evidence_size CHECK (file_size > 0 AND file_size <= 5242880)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS return_refund_request_events (
  event_id BIGINT NOT NULL AUTO_INCREMENT,
  request_id BIGINT NOT NULL,
  actor_user_id BIGINT NULL,
  from_status VARCHAR(32) NULL,
  to_status VARCHAR(32) NOT NULL,
  note VARCHAR(2000) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (event_id),
  KEY idx_return_refund_events_request (request_id, created_at, event_id),
  CONSTRAINT fk_return_refund_event_request FOREIGN KEY (request_id) REFERENCES return_refund_requests(request_id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_return_refund_event_actor FOREIGN KEY (actor_user_id) REFERENCES users(user_id) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Audit log for refund attempts; recording SUCCESS does not execute a bank/gateway transfer.
CREATE TABLE IF NOT EXISTS return_refund_transactions (
  transaction_id BIGINT NOT NULL AUTO_INCREMENT,
  request_id BIGINT NOT NULL,
  recorded_by_user_id BIGINT NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  method VARCHAR(40) NOT NULL,
  reference VARCHAR(120) NULL,
  status VARCHAR(16) NOT NULL,
  note VARCHAR(1000) NULL,
  recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (transaction_id),
  KEY idx_return_refund_transactions_request (request_id, recorded_at),
  UNIQUE KEY uq_return_refund_transaction_reference (method, reference),
  CONSTRAINT fk_return_refund_transaction_request FOREIGN KEY (request_id) REFERENCES return_refund_requests(request_id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_return_refund_transaction_actor FOREIGN KEY (recorded_by_user_id) REFERENCES users(user_id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT chk_return_refund_transaction_amount CHECK (amount > 0),
  CONSTRAINT chk_return_refund_transaction_status CHECK (status IN ('SUCCESS','FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Postflight: each query should return the four new workflow tables.
SELECT TABLE_NAME
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('return_refund_requests','return_refund_evidence',
                     'return_refund_request_events','return_refund_transactions')
ORDER BY TABLE_NAME;
