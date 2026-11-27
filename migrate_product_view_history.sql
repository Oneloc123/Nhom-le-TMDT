-- Ghi nhận lịch sử xem sản phẩm theo từng người dùng hoặc phiên truy cập
-- để phục vụ gợi ý cá nhân hóa thay vì chỉ dùng tổng products.view_count.
CREATE TABLE IF NOT EXISTS product_view_events (
    view_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    session_key VARCHAR(128) NULL,
    product_id BIGINT NOT NULL,
    viewed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_pve_user_time (user_id, viewed_at DESC),
    INDEX idx_pve_session_time (session_key, viewed_at DESC),
    INDEX idx_pve_product_time (product_id, viewed_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
