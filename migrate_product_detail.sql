-- Nâng cấp không phá dữ liệu cho chức năng chi tiết sản phẩm.
-- Chạy một lần trên database được tạo từ schema cũ.
ALTER TABLE reviews
    ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
