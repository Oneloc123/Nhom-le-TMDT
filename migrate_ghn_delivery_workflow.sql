-- Apply to the PrintKraft database after the existing shipping and support migrations.
CREATE TABLE IF NOT EXISTS ghn_deliveries (
    order_id BIGINT NOT NULL,
    province VARCHAR(100) NOT NULL,
    district VARCHAR(100) NULL,
    ward VARCHAR(100) NOT NULL,
    shipment_id BIGINT NULL,
    provider_status VARCHAR(40) NULL,
    provider_updated_at_ms BIGINT NOT NULL DEFAULT 0,
    next_check_at DATETIME NULL,
    confirm_by DATETIME NULL,
    PRIMARY KEY (order_id),
    UNIQUE KEY uq_ghn_delivery_shipment (shipment_id),
    KEY ix_ghn_delivery_poll (next_check_at),
    KEY ix_ghn_delivery_confirmation (confirm_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
