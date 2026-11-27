-- =============================================================================
-- PrintKraft — seed dữ liệu kiểm thử Dashboard Admin và trang Thống kê
-- =============================================================================
-- CHỈ DÙNG cho development/test. Tất cả bản ghi được đánh dấu bằng UI-SEED.
-- Không DROP/TRUNCATE bảng thật, không UPDATE/DELETE dữ liệu sẵn có.
--
-- Yêu cầu:
--   1) Chọn đúng database printkraft và sao lưu nếu đang dùng dữ liệu quan trọng.
--   2) Đã chạy schema printkraft_database_safe.sql.
--   3) Nếu kiểm thử hàng đợi mở rộng, chạy trước các migration tương ứng:
--      migrate_20261117_persistent_notifications_support_attachments.sql
--      migrate_admin_business_queues.sql
--      migrate_admin_operations_sla_manual_refunds.sql
--      docs/CUSTOM_PRINT_WORKFLOW_SCHEMA_PROPOSAL.sql (first-time/new schema)
--      OR docs/CUSTOM_PRINT_WORKFLOW_MIGRATION_EXISTING_MARIADB_10_4.sql (legacy DB)
--
-- Các bảng dữ liệu nghiệp vụ chính tạo khoảng 100 bản ghi/bảng để kiểm thử
-- pagination, bộ lọc, KPI, biểu đồ, top sản phẩm và các hàng đợi trên Dashboard.
-- File có thể chạy lại: mỗi INSERT kiểm tra dấu UI-SEED trước khi thêm bản ghi.
--
-- Tài khoản admin mẫu chỉ được tạo nếu chưa có:
--   admin@printkraft.vn / admin123
-- Tài khoản khách UI-SEED dùng cùng password hash mẫu 123456 của sample data.
-- Không dùng thông tin đăng nhập mẫu ở môi trường production.
-- =============================================================================

SET NAMES utf8mb4;
USE printkraft;

DROP TEMPORARY TABLE IF EXISTS tmp_pk_ui_seed_seq;
CREATE TEMPORARY TABLE tmp_pk_ui_seed_seq (
    n INT NOT NULL PRIMARY KEY
) ENGINE=MEMORY;

INSERT INTO tmp_pk_ui_seed_seq (n) VALUES
    (1),
    (2),
    (3),
    (4),
    (5),
    (6),
    (7),
    (8),
    (9),
    (10),
    (11),
    (12),
    (13),
    (14),
    (15),
    (16),
    (17),
    (18),
    (19),
    (20),
    (21),
    (22),
    (23),
    (24),
    (25),
    (26),
    (27),
    (28),
    (29),
    (30),
    (31),
    (32),
    (33),
    (34),
    (35),
    (36),
    (37),
    (38),
    (39),
    (40),
    (41),
    (42),
    (43),
    (44),
    (45),
    (46),
    (47),
    (48),
    (49),
    (50),
    (51),
    (52),
    (53),
    (54),
    (55),
    (56),
    (57),
    (58),
    (59),
    (60),
    (61),
    (62),
    (63),
    (64),
    (65),
    (66),
    (67),
    (68),
    (69),
    (70),
    (71),
    (72),
    (73),
    (74),
    (75),
    (76),
    (77),
    (78),
    (79),
    (80),
    (81),
    (82),
    (83),
    (84),
    (85),
    (86),
    (87),
    (88),
    (89),
    (90),
    (91),
    (92),
    (93),
    (94),
    (95),
    (96),
    (97),
    (98),
    (99),
    (100);

START TRANSACTION;

-- 1. Admin để gắn lịch sử duyệt, nếu hệ thống chưa có tài khoản mẫu.
INSERT INTO users (full_name, email, phone, password_hash, role, status, created_at)
SELECT 'PrintKraft UI Seed Admin', 'admin@printkraft.vn', '0901000001',
       'pbkdf2$210000$zrfD+LrRM1ld0BMdRCtLXQ==$4VtK1qg++9Poqf5RVyQvj70i3WiT6Fl6m7M/5FnEO6o=',
       'ADMIN', 'ACTIVE', NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@printkraft.vn');

SET @pk_ui_seed_admin_id := COALESCE(
    (SELECT user_id FROM users WHERE email = 'admin@printkraft.vn' LIMIT 1),
    (SELECT user_id FROM users WHERE UPPER(role) = 'ADMIN' AND UPPER(status) = 'ACTIVE' LIMIT 1)
);

-- 2. 100 khách hàng test.
INSERT INTO users (full_name, email, phone, password_hash, role, status, created_at, updated_at)
SELECT CONCAT('Khách hàng UI Seed ', LPAD(s.n, 3, '0')),
       CONCAT('ui.customer.', LPAD(s.n, 3, '0'), '@example.test'),
       CONCAT('09', LPAD(30000000 + s.n, 8, '0')),
       'pbkdf2$210000$YJN3Plki0bYpOHxSP2kiqA==$T6Dzel5zx/TumJomdpj4VF3dEHMvWLEKmlabGrZElQk=',
       'CUSTOMER',
       CASE WHEN MOD(s.n, 17) = 0 THEN 'LOCKED' ELSE 'ACTIVE' END,
       DATE_SUB(NOW(), INTERVAL MOD(s.n, 90) DAY),
       DATE_SUB(NOW(), INTERVAL MOD(s.n, 25) DAY)
FROM tmp_pk_ui_seed_seq s
WHERE NOT EXISTS (
    SELECT 1 FROM users u
    WHERE u.email = CONCAT('ui.customer.', LPAD(s.n, 3, '0'), '@example.test')
);

-- 3. 100 danh mục và 100 sản phẩm, gồm trạng thái chờ duyệt/đã duyệt/từ chối.
INSERT INTO categories (parent_id, category_name, slug, is_active, display_order)
SELECT NULL, CONCAT('Danh mục UI Seed ', LPAD(s.n, 3, '0')),
       CONCAT('ui-seed-category-', LPAD(s.n, 3, '0')), 1, s.n
FROM tmp_pk_ui_seed_seq s
WHERE NOT EXISTS (
    SELECT 1 FROM categories c
    WHERE c.slug = CONCAT('ui-seed-category-', LPAD(s.n, 3, '0'))
);

INSERT INTO products (
    category_id, product_name, slug, description, brand, base_unit,
    is_customizable, is_active, view_count, moderation_status, rejection_reason,
    reviewed_by_user_id, reviewed_at, search_keywords, created_at, updated_at
)
SELECT c.category_id,
       CONCAT('Sản phẩm in 3D UI Seed ', LPAD(s.n, 3, '0')),
       CONCAT('ui-seed-product-', LPAD(s.n, 3, '0')),
       CONCAT('Dữ liệu minh họa phục vụ kiểm thử giao diện. Mã UI-SEED-', LPAD(s.n, 3, '0')),
       'PrintKraft UI Seed', 'cái', MOD(s.n, 2), 
       CASE WHEN MOD(s.n, 10) < 2 OR MOD(s.n, 10) = 2 THEN 0 ELSE 1 END,
       100 + s.n * 13,
       CASE WHEN MOD(s.n, 10) < 2 THEN 'PENDING_REVIEW'
            WHEN MOD(s.n, 10) = 2 THEN 'REJECTED'
            ELSE 'APPROVED' END,
       CASE WHEN MOD(s.n, 10) = 2 THEN 'UI-SEED: cần bổ sung thông tin sản phẩm' ELSE NULL END,
       CASE WHEN MOD(s.n, 10) < 2 THEN NULL ELSE @pk_ui_seed_admin_id END,
       CASE WHEN MOD(s.n, 10) < 2 THEN NULL ELSE DATE_SUB(NOW(), INTERVAL MOD(s.n, 30) DAY) END,
       CONCAT('UI-SEED in 3D FDM PLA resin prototype ', s.n),
       DATE_SUB(NOW(), INTERVAL MOD(s.n, 90) DAY),
       DATE_SUB(NOW(), INTERVAL MOD(s.n, 20) DAY)
FROM tmp_pk_ui_seed_seq s
JOIN categories c ON c.slug = CONCAT('ui-seed-category-', LPAD(s.n, 3, '0'))
WHERE NOT EXISTS (
    SELECT 1 FROM products p
    WHERE p.slug = CONCAT('ui-seed-product-', LPAD(s.n, 3, '0'))
);

-- 4. 100 xưởng in, trong đó có xưởng chờ xác minh và bị hạn chế để test bộ lọc.
INSERT INTO print_shops (
    owner_user_id, shop_name, slug, description, logo_url, contact_phone, contact_email,
    address_line, province, district, search_keywords, capabilities, accepted_file_formats,
    max_build_dimensions, operating_hours, estimated_lead_days, service_notes,
    average_rating, verification_status, status, created_at
)
SELECT u.user_id,
       CONCAT('Xưởng UI Seed ', LPAD(s.n, 3, '0')),
       CONCAT('ui-seed-shop-', LPAD(s.n, 3, '0')),
       CONCAT('Xưởng in kiểm thử giao diện số ', s.n, '. Không phải xưởng dịch vụ thật.'),
       NULL, u.phone, u.email,
       CONCAT(s.n, ' đường Dữ liệu Mẫu'),
       CASE MOD(s.n, 5)
         WHEN 0 THEN 'TP. Hồ Chí Minh'
         WHEN 1 THEN 'Hà Nội'
         WHEN 2 THEN 'Đà Nẵng'
         WHEN 3 THEN 'Bình Dương'
         ELSE 'Đồng Nai'
       END,
       CASE MOD(s.n, 4)
         WHEN 0 THEN 'Quận 1'
         WHEN 1 THEN 'Cầu Giấy'
         WHEN 2 THEN 'Hải Châu'
         ELSE 'Thủ Đức'
       END,
       CONCAT('UI-SEED FDM SLA resin PLA PETG ', s.n),
       'FDM, SLA, PETG, PLA, Resin',
       'STL, OBJ, 3MF',
       '220x220x250 mm', '08:00-18:00', 2 + MOD(s.n, 8),
       'Thông tin và năng lực mẫu, cần xác nhận trước giao dịch thật.',
       3.00 + MOD(s.n, 21) / 10,
       CASE MOD(s.n, 5)
         WHEN 0 THEN 'PENDING'
         WHEN 1 THEN 'REJECTED'
         ELSE 'VERIFIED'
       END,
       CASE WHEN MOD(s.n, 7) = 0 THEN 'INACTIVE' ELSE 'ACTIVE' END,
       DATE_SUB(NOW(), INTERVAL MOD(s.n, 90) DAY)
FROM tmp_pk_ui_seed_seq s
JOIN users u ON u.email = CONCAT('ui.customer.', LPAD(s.n, 3, '0'), '@example.test')
WHERE NOT EXISTS (
    SELECT 1 FROM print_shops p
    WHERE p.slug = CONCAT('ui-seed-shop-', LPAD(s.n, 3, '0'))
);

-- 5. 100 chào bán, 100 ảnh sản phẩm, 100 bộ lọc, 100 option và 100 giá trị lọc.
INSERT INTO shop_products (
    shop_id, product_id, sku, printing_method, material, unit_price,
    minimum_quantity, production_days, stock_status, is_active, updated_at
)
SELECT sh.shop_id, p.product_id,
       CONCAT('UI-SEED-SKU-', LPAD(s.n, 3, '0')),
       CASE MOD(s.n, 3) WHEN 0 THEN 'SLA' WHEN 1 THEN 'FDM' ELSE 'FDM' END,
       CASE MOD(s.n, 4) WHEN 0 THEN 'PETG' WHEN 1 THEN 'PLA' WHEN 2 THEN 'Resin' ELSE 'ABS' END,
       70000 + s.n * 5000, 1 + MOD(s.n, 3), 2 + MOD(s.n, 7),
       CASE WHEN MOD(s.n, 6) = 0 THEN 'OUT_OF_STOCK' ELSE 'IN_STOCK' END,
       CASE WHEN MOD(s.n, 10) = 0 THEN 0 ELSE 1 END,
       DATE_SUB(NOW(), INTERVAL MOD(s.n, 15) DAY)
FROM tmp_pk_ui_seed_seq s
JOIN print_shops sh ON sh.slug = CONCAT('ui-seed-shop-', LPAD(s.n, 3, '0'))
JOIN products p ON p.slug = CONCAT('ui-seed-product-', LPAD(s.n, 3, '0'))
WHERE NOT EXISTS (
    SELECT 1 FROM shop_products sp
    WHERE sp.sku = CONCAT('UI-SEED-SKU-', LPAD(s.n, 3, '0'))
);

INSERT INTO product_images (product_id, image_url, alt_text, is_primary, display_order)
SELECT p.product_id,
       CONCAT('https://placehold.co/900x650?text=PrintKraft+UI+Seed+', LPAD(s.n, 3, '0')),
       CONCAT('Ảnh mẫu sản phẩm UI Seed ', LPAD(s.n, 3, '0')),
       1, 1
FROM tmp_pk_ui_seed_seq s
JOIN products p ON p.slug = CONCAT('ui-seed-product-', LPAD(s.n, 3, '0'))
WHERE NOT EXISTS (
    SELECT 1 FROM product_images i
    WHERE i.product_id = p.product_id
      AND i.image_url = CONCAT('https://placehold.co/900x650?text=PrintKraft+UI+Seed+', LPAD(s.n, 3, '0'))
);

INSERT INTO product_filters (filter_code, filter_name, filter_type, display_order, is_active)
SELECT CONCAT('UI-SEED-FILTER-', LPAD(s.n, 3, '0')),
       CONCAT('Thuộc tính UI Seed ', LPAD(s.n, 3, '0')),
       'SELECT', s.n, 1
FROM tmp_pk_ui_seed_seq s
WHERE NOT EXISTS (
    SELECT 1 FROM product_filters f
    WHERE f.filter_code = CONCAT('UI-SEED-FILTER-', LPAD(s.n, 3, '0'))
);

INSERT INTO product_filter_options (filter_id, option_label, option_value, display_order, is_active)
SELECT f.filter_id,
       CONCAT('Lựa chọn UI Seed ', LPAD(s.n, 3, '0')),
       CONCAT('ui_seed_option_', LPAD(s.n, 3, '0')),
       1, 1
FROM tmp_pk_ui_seed_seq s
JOIN product_filters f ON f.filter_code = CONCAT('UI-SEED-FILTER-', LPAD(s.n, 3, '0'))
WHERE NOT EXISTS (
    SELECT 1 FROM product_filter_options o
    WHERE o.filter_id = f.filter_id
      AND o.option_value = CONCAT('ui_seed_option_', LPAD(s.n, 3, '0'))
);

INSERT INTO product_filter_values (product_id, option_id, value_text, numeric_value)
SELECT p.product_id, o.option_id,
       CONCAT('UI-SEED value ', LPAD(s.n, 3, '0')), s.n * 10
FROM tmp_pk_ui_seed_seq s
JOIN products p ON p.slug = CONCAT('ui-seed-product-', LPAD(s.n, 3, '0'))
JOIN product_filters f ON f.filter_code = CONCAT('UI-SEED-FILTER-', LPAD(s.n, 3, '0'))
JOIN product_filter_options o ON o.filter_id = f.filter_id
    AND o.option_value = CONCAT('ui_seed_option_', LPAD(s.n, 3, '0'))
WHERE NOT EXISTS (
    SELECT 1 FROM product_filter_values v
    WHERE v.product_id = p.product_id AND v.option_id = o.option_id
);

-- 6. 100 địa chỉ, giỏ hàng, tệp thiết kế, dòng giỏ và mục yêu thích.
INSERT INTO addresses (user_id, recipient_name, recipient_phone, province, district, address_line, is_default)
SELECT u.user_id, u.full_name, u.phone,
       CASE MOD(s.n, 4)
         WHEN 0 THEN 'TP. Hồ Chí Minh'
         WHEN 1 THEN 'Hà Nội'
         WHEN 2 THEN 'Đà Nẵng'
         ELSE 'Cần Thơ'
       END,
       CASE MOD(s.n, 4)
         WHEN 0 THEN 'Quận 1'
         WHEN 1 THEN 'Cầu Giấy'
         WHEN 2 THEN 'Hải Châu'
         ELSE 'Ninh Kiều'
       END,
       CONCAT('UI Seed Address ', LPAD(s.n, 3, '0')),
       1
FROM tmp_pk_ui_seed_seq s
JOIN users u ON u.email = CONCAT('ui.customer.', LPAD(s.n, 3, '0'), '@example.test')
WHERE NOT EXISTS (
    SELECT 1 FROM addresses a
    WHERE a.user_id = u.user_id
      AND a.address_line = CONCAT('UI Seed Address ', LPAD(s.n, 3, '0'))
);

INSERT INTO carts (user_id, status, currency, subtotal, expires_at, created_at, updated_at)
SELECT u.user_id, 'ACTIVE', 'VND', 70000 + s.n * 5000, NULL,
       DATE_SUB(NOW(), INTERVAL MOD(s.n, 15) DAY), NOW()
FROM tmp_pk_ui_seed_seq s
JOIN users u ON u.email = CONCAT('ui.customer.', LPAD(s.n, 3, '0'), '@example.test')
WHERE NOT EXISTS (SELECT 1 FROM carts c WHERE c.user_id = u.user_id);

INSERT INTO design_files (
    user_id, product_id, original_filename, storage_url, preview_url, status, created_at
)
SELECT u.user_id, p.product_id,
       CONCAT('ui-seed-design-', LPAD(s.n, 3, '0'), '.stl'),
       CONCAT('sample://ui-seed/design-', LPAD(s.n, 3, '0'), '.stl'),
       CONCAT('https://placehold.co/500x400?text=3D+Design+', LPAD(s.n, 3, '0')),
       CASE WHEN MOD(s.n, 8) = 0 THEN 'PROCESSING' ELSE 'READY' END,
       DATE_SUB(NOW(), INTERVAL MOD(s.n, 60) DAY)
FROM tmp_pk_ui_seed_seq s
JOIN users u ON u.email = CONCAT('ui.customer.', LPAD(s.n, 3, '0'), '@example.test')
JOIN products p ON p.slug = CONCAT('ui-seed-product-', LPAD(s.n, 3, '0'))
WHERE NOT EXISTS (
    SELECT 1 FROM design_files f
    WHERE f.user_id = u.user_id
      AND f.original_filename = CONCAT('ui-seed-design-', LPAD(s.n, 3, '0'), '.stl')
);

INSERT INTO cart_items (
    cart_id, shop_product_id, design_file_id, quantity, selected_options,
    unit_price_snapshot, line_total, created_at
)
SELECT c.cart_id, sp.shop_product_id, df.design_file_id,
       1 + MOD(s.n, 4),
       JSON_OBJECT('source', 'UI-SEED', 'color', CASE WHEN MOD(s.n, 2) = 0 THEN 'Black' ELSE 'White' END),
       sp.unit_price,
       sp.unit_price * (1 + MOD(s.n, 4)),
       DATE_SUB(NOW(), INTERVAL MOD(s.n, 10) DAY)
FROM tmp_pk_ui_seed_seq s
JOIN users u ON u.email = CONCAT('ui.customer.', LPAD(s.n, 3, '0'), '@example.test')
JOIN carts c ON c.user_id = u.user_id
JOIN shop_products sp ON sp.sku = CONCAT('UI-SEED-SKU-', LPAD(s.n, 3, '0'))
JOIN design_files df ON df.user_id = u.user_id
    AND df.original_filename = CONCAT('ui-seed-design-', LPAD(s.n, 3, '0'), '.stl')
WHERE NOT EXISTS (
    SELECT 1 FROM cart_items ci
    WHERE ci.cart_id = c.cart_id
      AND ci.shop_product_id = sp.shop_product_id
      AND ci.design_file_id = df.design_file_id
);

INSERT INTO favorites (user_id, product_id, created_at)
SELECT u.user_id, p.product_id, DATE_SUB(NOW(), INTERVAL MOD(s.n, 60) DAY)
FROM tmp_pk_ui_seed_seq s
JOIN users u ON u.email = CONCAT('ui.customer.', LPAD(s.n, 3, '0'), '@example.test')
JOIN products p ON p.slug = CONCAT('ui-seed-product-', LPAD(s.n, 3, '0'))
WHERE NOT EXISTS (
    SELECT 1 FROM favorites f
    WHERE f.user_id = u.user_id AND f.product_id = p.product_id
);

-- 7. 100 đơn hàng, 100 dòng hàng, 100 thanh toán và 100 bản ghi lịch sử.
INSERT INTO orders (
    order_code, user_id, address_id,
    recipient_name_snapshot, recipient_phone_snapshot, shipping_address_snapshot,
    subtotal, shipping_fee, total_amount, currency,
    status, payment_status, shipping_status, placed_at, completed_at, cancelled_at
)
SELECT CONCAT('UI-SEED-ORDER-', LPAD(s.n, 3, '0')),
       u.user_id, a.address_id,
       u.full_name, u.phone,
       CONCAT(a.address_line, ', ', a.district, ', ', a.province),
       (70000 + s.n * 5000) * (1 + MOD(s.n, 4)),
       15000 + MOD(s.n, 5) * 5000,
       ((70000 + s.n * 5000) * (1 + MOD(s.n, 4))) + 15000 + MOD(s.n, 5) * 5000,
       'VND',
       CASE MOD(s.n, 6)
         WHEN 0 THEN 'PENDING_PAYMENT'
         WHEN 1 THEN 'CONFIRMED'
         WHEN 2 THEN 'PROCESSING'
         WHEN 3 THEN 'SHIPPED'
         WHEN 4 THEN 'COMPLETED'
         ELSE 'CANCELLED'
       END,
       CASE MOD(s.n, 6)
         WHEN 0 THEN 'PENDING'
         WHEN 5 THEN 'REFUND_REQUIRED'
         ELSE 'PAID'
       END,
       CASE MOD(s.n, 6)
         WHEN 0 THEN 'NOT_SHIPPED'
         WHEN 1 THEN 'PENDING'
         WHEN 2 THEN 'PROCESSING'
         WHEN 3 THEN 'IN_TRANSIT'
         WHEN 4 THEN 'DELIVERED'
         ELSE 'CANCELLED'
       END,
       DATE_SUB(NOW(), INTERVAL MOD(s.n, 75) DAY),
       CASE WHEN MOD(s.n, 6) = 4 THEN DATE_SUB(NOW(), INTERVAL MOD(s.n, 20) DAY) ELSE NULL END,
       CASE WHEN MOD(s.n, 6) = 5 THEN DATE_SUB(NOW(), INTERVAL MOD(s.n, 20) DAY) ELSE NULL END
FROM tmp_pk_ui_seed_seq s
JOIN users u ON u.email = CONCAT('ui.customer.', LPAD(s.n, 3, '0'), '@example.test')
JOIN addresses a ON a.user_id = u.user_id
    AND a.address_line = CONCAT('UI Seed Address ', LPAD(s.n, 3, '0'))
WHERE NOT EXISTS (
    SELECT 1 FROM orders o
    WHERE o.order_code = CONCAT('UI-SEED-ORDER-', LPAD(s.n, 3, '0'))
);

INSERT INTO order_items (
    order_id, product_id, shop_product_id, shop_id, design_file_id,
    product_name_snapshot, shop_name_snapshot, quantity, unit_price, line_total, status
)
SELECT o.order_id, p.product_id, sp.shop_product_id, sh.shop_id, df.design_file_id,
       p.product_name, sh.shop_name, 1 + MOD(s.n, 4),
       sp.unit_price,
       sp.unit_price * (1 + MOD(s.n, 4)),
       o.status
FROM tmp_pk_ui_seed_seq s
JOIN orders o ON o.order_code = CONCAT('UI-SEED-ORDER-', LPAD(s.n, 3, '0'))
JOIN products p ON p.slug = CONCAT('ui-seed-product-', LPAD(s.n, 3, '0'))
JOIN shop_products sp ON sp.sku = CONCAT('UI-SEED-SKU-', LPAD(s.n, 3, '0'))
JOIN print_shops sh ON sh.slug = CONCAT('ui-seed-shop-', LPAD(s.n, 3, '0'))
JOIN design_files df ON df.original_filename = CONCAT('ui-seed-design-', LPAD(s.n, 3, '0'), '.stl')
    AND df.user_id = o.user_id
WHERE NOT EXISTS (
    SELECT 1 FROM order_items oi
    WHERE oi.order_id = o.order_id AND oi.shop_product_id = sp.shop_product_id
);

INSERT INTO payments (
    order_id, payment_method, provider, provider_transaction_ref, provider_transaction_no,
    response_code, bank_code, card_type, checkout_url, amount, status,
    failure_reason, paid_at, created_at, updated_at
)
SELECT o.order_id,
       CASE MOD(s.n, 4) WHEN 0 THEN 'COD' WHEN 1 THEN 'VNPAY' WHEN 2 THEN 'ZALOPAY' ELSE 'BANK_TRANSFER' END,
       CASE MOD(s.n, 4) WHEN 0 THEN 'COD' WHEN 1 THEN 'VNPAY' WHEN 2 THEN 'ZALOPAY' ELSE 'BANK_TRANSFER' END,
       CONCAT('UI-SEED-PAYMENT-', LPAD(s.n, 3, '0')),
       CONCAT('UISEEDTX', LPAD(s.n, 6, '0')),
       CASE WHEN MOD(s.n, 6) = 0 THEN '00' WHEN MOD(s.n, 6) = 5 THEN 'RF' ELSE '00' END,
       CASE MOD(s.n, 4) WHEN 1 THEN 'NCB' WHEN 2 THEN 'ZLP' ELSE NULL END,
       CASE WHEN MOD(s.n, 4) = 1 THEN 'DEMO' ELSE NULL END,
       NULL, o.total_amount,
       CASE MOD(s.n, 6)
         WHEN 0 THEN 'PENDING'
         WHEN 5 THEN 'REFUND_REQUIRED'
         ELSE 'PAID'
       END,
       CASE WHEN MOD(s.n, 6) = 5 THEN 'UI-SEED: cần kiểm tra hoàn tiền, không phải giao dịch thật' ELSE NULL END,
       CASE WHEN MOD(s.n, 6) IN (1, 2, 3, 4) THEN o.placed_at ELSE NULL END,
       o.placed_at, o.placed_at
FROM tmp_pk_ui_seed_seq s
JOIN orders o ON o.order_code = CONCAT('UI-SEED-ORDER-', LPAD(s.n, 3, '0'))
WHERE NOT EXISTS (
    SELECT 1 FROM payments p
    WHERE p.provider_transaction_ref = CONCAT('UI-SEED-PAYMENT-', LPAD(s.n, 3, '0'))
);

INSERT INTO order_status_history (order_id, changed_by_user_id, old_status, new_status, note, changed_at)
SELECT o.order_id, @pk_ui_seed_admin_id,
       'UI-SEED_CREATED', o.status,
       CONCAT('UI-SEED order history ', LPAD(s.n, 3, '0')),
       DATE_SUB(o.placed_at, INTERVAL 1 MINUTE)
FROM tmp_pk_ui_seed_seq s
JOIN orders o ON o.order_code = CONCAT('UI-SEED-ORDER-', LPAD(s.n, 3, '0'))
WHERE NOT EXISTS (
    SELECT 1 FROM order_status_history h
    WHERE h.order_id = o.order_id
      AND h.note = CONCAT('UI-SEED order history ', LPAD(s.n, 3, '0'))
);

-- 8. 100 yêu cầu báo giá và thanh toán báo giá.
INSERT INTO quote_requests (
    user_id, product_id, preferred_shop_id, company_name, requested_quantity,
    required_by, quoted_amount, deposit_amount, deposit_paid_at, production_started_at,
    completed_at, production_note, order_id, seller_response_note, status
)
SELECT u.user_id, p.product_id, sh.shop_id,
       CONCAT('UI Seed Company ', LPAD(s.n, 3, '0')),
       10 + MOD(s.n * 7, 500),
       DATE_ADD(CURRENT_DATE, INTERVAL MOD(s.n, 60) DAY),
       CASE WHEN MOD(s.n, 4) < 2 THEN NULL ELSE 200000 + s.n * 30000 END,
       CASE WHEN MOD(s.n, 4) < 2 THEN NULL ELSE 50000 + s.n * 5000 END,
       CASE WHEN MOD(s.n, 4) = 2 THEN DATE_SUB(NOW(), INTERVAL MOD(s.n, 10) DAY) ELSE NULL END,
       CASE WHEN MOD(s.n, 4) = 3 THEN DATE_SUB(NOW(), INTERVAL MOD(s.n, 10) DAY) ELSE NULL END,
       NULL,
       CONCAT('UI-SEED: báo giá minh họa số ', LPAD(s.n, 3, '0')),
       NULL,
       CASE WHEN MOD(s.n, 4) < 2 THEN NULL ELSE 'Báo giá mẫu dùng kiểm thử giao diện.' END,
       CASE MOD(s.n, 4)
         WHEN 0 THEN 'PENDING'
         WHEN 1 THEN 'PENDING_REVIEW'
         WHEN 2 THEN 'QUOTED'
         ELSE 'COMPLETED'
       END
FROM tmp_pk_ui_seed_seq s
JOIN users u ON u.email = CONCAT('ui.customer.', LPAD(s.n, 3, '0'), '@example.test')
JOIN products p ON p.slug = CONCAT('ui-seed-product-', LPAD(s.n, 3, '0'))
JOIN print_shops sh ON sh.slug = CONCAT('ui-seed-shop-', LPAD(s.n, 3, '0'))
WHERE NOT EXISTS (
    SELECT 1 FROM quote_requests q
    WHERE q.user_id = u.user_id
      AND q.company_name = CONCAT('UI Seed Company ', LPAD(s.n, 3, '0'))
);

INSERT INTO quote_payments (
    quote_request_id, user_id, provider, app_trans_id, amount, status,
    return_code, return_message, channel, paid_at, created_at, updated_at
)
SELECT q.quote_request_id, q.user_id, 'ZALOPAY',
       CONCAT('UISEEDQUOTE', LPAD(s.n, 6, '0')),
       50000 + s.n * 5000,
       CASE MOD(s.n, 4) WHEN 0 THEN 'SUCCESS' WHEN 1 THEN 'PENDING' WHEN 2 THEN 'FAILED' ELSE 'PENDING' END,
       CASE MOD(s.n, 4) WHEN 0 THEN 1 WHEN 2 THEN 0 ELSE NULL END,
       CASE MOD(s.n, 4) WHEN 0 THEN 'UI-SEED: mô phỏng thành công' WHEN 2 THEN 'UI-SEED: mô phỏng thất bại' ELSE 'UI-SEED: đang chờ' END,
       1,
       CASE WHEN MOD(s.n, 4) = 0 THEN q.required_by ELSE NULL END,
       DATE_SUB(NOW(), INTERVAL MOD(s.n, 20) DAY), NOW()
FROM tmp_pk_ui_seed_seq s
JOIN quote_requests q
  ON q.company_name = CONCAT('UI Seed Company ', LPAD(s.n, 3, '0'))
JOIN users u ON u.user_id = q.user_id
WHERE NOT EXISTS (
    SELECT 1 FROM quote_payments qp
    WHERE qp.app_trans_id = CONCAT('UISEEDQUOTE', LPAD(s.n, 6, '0'))
);

-- 9. 100 cập nhật sản xuất, 100 vận đơn, 100 sự kiện vận đơn.
INSERT INTO production_updates (
    order_item_id, shop_id, created_by_user_id, stage, progress_percent, message, created_at
)
SELECT oi.order_item_id, oi.shop_id, @pk_ui_seed_admin_id,
       CASE MOD(s.n, 4)
         WHEN 0 THEN 'DESIGN'
         WHEN 1 THEN 'PRINTING'
         WHEN 2 THEN 'POST_PROCESSING'
         ELSE 'QUALITY_CHECK'
       END,
       MOD(s.n * 7, 101),
       CONCAT('UI-SEED: cập nhật sản xuất ', LPAD(s.n, 3, '0')),
       DATE_SUB(NOW(), INTERVAL MOD(s.n, 20) DAY)
FROM tmp_pk_ui_seed_seq s
JOIN orders o ON o.order_code = CONCAT('UI-SEED-ORDER-', LPAD(s.n, 3, '0'))
JOIN order_items oi ON oi.order_id = o.order_id
WHERE NOT EXISTS (
    SELECT 1 FROM production_updates pu
    WHERE pu.order_item_id = oi.order_item_id
      AND pu.message = CONCAT('UI-SEED: cập nhật sản xuất ', LPAD(s.n, 3, '0'))
);

INSERT INTO shipments (order_id, shop_id, carrier, tracking_number, status, shipped_at, delivered_at)
SELECT o.order_id, sh.shop_id,
       CASE MOD(s.n, 3) WHEN 0 THEN 'GHN' WHEN 1 THEN 'GHTK' ELSE 'Viettel Post' END,
       CONCAT('UISEEDTRACK', LPAD(s.n, 6, '0')),
       CASE MOD(s.n, 6)
         WHEN 0 THEN 'PENDING'
         WHEN 1 THEN 'READY_TO_SHIP'
         WHEN 2 THEN 'PROCESSING'
         WHEN 3 THEN 'IN_TRANSIT'
         WHEN 4 THEN 'DELIVERED'
         ELSE 'CANCELLED'
       END,
       CASE WHEN MOD(s.n, 6) IN (3, 4) THEN DATE_SUB(NOW(), INTERVAL MOD(s.n, 10) DAY) ELSE NULL END,
       CASE WHEN MOD(s.n, 6) = 4 THEN DATE_SUB(NOW(), INTERVAL MOD(s.n, 5) DAY) ELSE NULL END
FROM tmp_pk_ui_seed_seq s
JOIN orders o ON o.order_code = CONCAT('UI-SEED-ORDER-', LPAD(s.n, 3, '0'))
JOIN print_shops sh ON sh.slug = CONCAT('ui-seed-shop-', LPAD(s.n, 3, '0'))
WHERE NOT EXISTS (
    SELECT 1 FROM shipments sp
    WHERE sp.tracking_number = CONCAT('UISEEDTRACK', LPAD(s.n, 6, '0'))
);

INSERT INTO shipment_events (order_id, shipment_id, event_title, description, occurred_at)
SELECT o.order_id, sh.shipment_id,
       CONCAT('UI-SEED shipment event ', LPAD(s.n, 3, '0')),
       'Sự kiện vận chuyển minh họa để kiểm thử giao diện.',
       DATE_SUB(NOW(), INTERVAL MOD(s.n, 15) DAY)
FROM tmp_pk_ui_seed_seq s
JOIN orders o ON o.order_code = CONCAT('UI-SEED-ORDER-', LPAD(s.n, 3, '0'))
JOIN shipments sh ON sh.tracking_number = CONCAT('UISEEDTRACK', LPAD(s.n, 6, '0'))
WHERE NOT EXISTS (
    SELECT 1 FROM shipment_events e
    WHERE e.order_id = o.order_id
      AND e.event_title = CONCAT('UI-SEED shipment event ', LPAD(s.n, 3, '0'))
);

-- 10. 100 đánh giá, cố ý giữ một phần chưa xuất bản để kiểm thử kiểm duyệt.
INSERT INTO reviews (order_item_id, user_id, product_id, shop_id, rating, review_text, is_published, created_at)
SELECT oi.order_item_id, o.user_id, oi.product_id, oi.shop_id,
       1 + MOD(s.n, 5),
       CONCAT('UI-SEED review ', LPAD(s.n, 3, '0'),
              ': đánh giá minh họa, không đại diện cho khách hàng thật.'),
       CASE WHEN MOD(s.n, 4) = 0 THEN 0 ELSE 1 END,
       DATE_SUB(NOW(), INTERVAL MOD(s.n, 60) DAY)
FROM tmp_pk_ui_seed_seq s
JOIN orders o ON o.order_code = CONCAT('UI-SEED-ORDER-', LPAD(s.n, 3, '0'))
JOIN order_items oi ON oi.order_id = o.order_id
WHERE NOT EXISTS (
    SELECT 1 FROM reviews r WHERE r.order_item_id = oi.order_item_id
);

INSERT INTO admin_review_moderation_events (review_id, admin_user_id, action, reason, created_at)
SELECT r.review_id, @pk_ui_seed_admin_id,
       CASE WHEN r.is_published = 1 THEN 'PUBLISH' ELSE 'HIDE' END,
       CONCAT('UI-SEED review moderation ', LPAD(s.n, 3, '0')),
       DATE_SUB(NOW(), INTERVAL MOD(s.n, 30) DAY)
FROM tmp_pk_ui_seed_seq s
JOIN orders o ON o.order_code = CONCAT('UI-SEED-ORDER-', LPAD(s.n, 3, '0'))
JOIN order_items oi ON oi.order_id = o.order_id
JOIN reviews r ON r.order_item_id = oi.order_item_id
WHERE NOT EXISTS (
    SELECT 1 FROM admin_review_moderation_events e
    WHERE e.review_id = r.review_id
      AND e.reason = CONCAT('UI-SEED review moderation ', LPAD(s.n, 3, '0'))
);

-- 11. 100 ticket hỗ trợ, tin nhắn, tệp đính kèm, sự kiện và thông báo.
INSERT INTO support_tickets (
    ticket_code, user_id, order_id, shop_id, category, subject, priority, status,
    assigned_admin_user_id, sla_due_at, created_at, updated_at, resolved_at, closed_at, customer_rating
)
SELECT CONCAT('UI-SEED-TICKET-', LPAD(s.n, 3, '0')),
       u.user_id, o.order_id, sh.shop_id,
       CASE MOD(s.n, 5)
         WHEN 0 THEN 'ORDER'
         WHEN 1 THEN 'PAYMENT'
         WHEN 2 THEN 'SHIPPING'
         WHEN 3 THEN 'PRODUCT'
         ELSE 'OTHER'
       END,
       CONCAT('Yêu cầu hỗ trợ UI Seed ', LPAD(s.n, 3, '0')),
       CASE MOD(s.n, 3) WHEN 0 THEN 'HIGH' WHEN 1 THEN 'NORMAL' ELSE 'LOW' END,
       CASE MOD(s.n, 5)
         WHEN 0 THEN 'OPEN'
         WHEN 1 THEN 'IN_PROGRESS'
         WHEN 2 THEN 'WAITING_CUSTOMER'
         WHEN 3 THEN 'RESOLVED'
         ELSE 'CLOSED'
       END,
       CASE WHEN MOD(s.n, 5) IN (0, 1, 2) THEN @pk_ui_seed_admin_id ELSE NULL END,
       DATE_ADD(NOW(), INTERVAL MOD(s.n, 24) HOUR),
       DATE_SUB(NOW(), INTERVAL MOD(s.n, 45) DAY),
       DATE_SUB(NOW(), INTERVAL MOD(s.n, 10) DAY),
       CASE WHEN MOD(s.n, 5) IN (3, 4) THEN DATE_SUB(NOW(), INTERVAL MOD(s.n, 5) DAY) ELSE NULL END,
       CASE WHEN MOD(s.n, 5) = 4 THEN DATE_SUB(NOW(), INTERVAL MOD(s.n, 3) DAY) ELSE NULL END,
       CASE WHEN MOD(s.n, 5) = 4 THEN 3 + MOD(s.n, 3) ELSE NULL END
FROM tmp_pk_ui_seed_seq s
JOIN users u ON u.email = CONCAT('ui.customer.', LPAD(s.n, 3, '0'), '@example.test')
JOIN orders o ON o.order_code = CONCAT('UI-SEED-ORDER-', LPAD(s.n, 3, '0'))
JOIN print_shops sh ON sh.slug = CONCAT('ui-seed-shop-', LPAD(s.n, 3, '0'))
WHERE NOT EXISTS (
    SELECT 1 FROM support_tickets t
    WHERE t.ticket_code = CONCAT('UI-SEED-TICKET-', LPAD(s.n, 3, '0'))
);

INSERT INTO support_ticket_messages (ticket_id, sender_user_id, message_body, is_internal, created_at)
SELECT t.ticket_id,
       CASE WHEN MOD(s.n, 2) = 0 THEN @pk_ui_seed_admin_id ELSE u.user_id END,
       CONCAT('UI-SEED message ', LPAD(s.n, 3, '0'),
              ': nội dung hội thoại mẫu phục vụ kiểm thử hỗ trợ.'),
       CASE WHEN MOD(s.n, 4) = 0 THEN 1 ELSE 0 END,
       DATE_SUB(NOW(), INTERVAL MOD(s.n, 20) DAY)
FROM tmp_pk_ui_seed_seq s
JOIN users u ON u.email = CONCAT('ui.customer.', LPAD(s.n, 3, '0'), '@example.test')
JOIN support_tickets t ON t.ticket_code = CONCAT('UI-SEED-TICKET-', LPAD(s.n, 3, '0'))
WHERE NOT EXISTS (
    SELECT 1 FROM support_ticket_messages m
    WHERE m.ticket_id = t.ticket_id
      AND m.message_body = CONCAT('UI-SEED message ', LPAD(s.n, 3, '0'),
                                  ': nội dung hội thoại mẫu phục vụ kiểm thử hỗ trợ.')
);

INSERT INTO support_ticket_attachments (
    ticket_id, uploaded_by_user_id, original_filename, content_type, file_size, file_data, created_at
)
SELECT t.ticket_id, u.user_id,
       CONCAT('ui-seed-attachment-', LPAD(s.n, 3, '0'), '.txt'),
       'text/plain',
       LENGTH(CONCAT('UI-SEED attachment ', LPAD(s.n, 3, '0'))),
       CONCAT('UI-SEED attachment ', LPAD(s.n, 3, '0')),
       DATE_SUB(NOW(), INTERVAL MOD(s.n, 15) DAY)
FROM tmp_pk_ui_seed_seq s
JOIN users u ON u.email = CONCAT('ui.customer.', LPAD(s.n, 3, '0'), '@example.test')
JOIN support_tickets t ON t.ticket_code = CONCAT('UI-SEED-TICKET-', LPAD(s.n, 3, '0'))
WHERE NOT EXISTS (
    SELECT 1 FROM support_ticket_attachments a
    WHERE a.ticket_id = t.ticket_id
      AND a.original_filename = CONCAT('ui-seed-attachment-', LPAD(s.n, 3, '0'), '.txt')
);

INSERT INTO support_ticket_events (
    ticket_id, admin_user_id, old_status, new_status, old_priority, new_priority,
    old_assigned_admin_user_id, new_assigned_admin_user_id, note, created_at
)
SELECT t.ticket_id, @pk_ui_seed_admin_id,
       'OPEN', t.status, 'NORMAL', t.priority,
       NULL, t.assigned_admin_user_id,
       CONCAT('UI-SEED ticket event ', LPAD(s.n, 3, '0')),
       DATE_SUB(NOW(), INTERVAL MOD(s.n, 15) DAY)
FROM tmp_pk_ui_seed_seq s
JOIN support_tickets t ON t.ticket_code = CONCAT('UI-SEED-TICKET-', LPAD(s.n, 3, '0'))
WHERE NOT EXISTS (
    SELECT 1 FROM support_ticket_events e
    WHERE e.ticket_id = t.ticket_id
      AND e.note = CONCAT('UI-SEED ticket event ', LPAD(s.n, 3, '0'))
);

INSERT INTO notifications (
    user_id, notification_type, title, message, target_url,
    related_type, related_id, is_read, created_at
)
SELECT u.user_id,
       CASE MOD(s.n, 4) WHEN 0 THEN 'ORDER_UPDATE' WHEN 1 THEN 'PAYMENT' WHEN 2 THEN 'SUPPORT' ELSE 'SYSTEM' END,
       CONCAT('UI-SEED thông báo ', LPAD(s.n, 3, '0')),
       CONCAT('Thông báo minh họa để kiểm thử danh sách và trạng thái đã đọc: ', LPAD(s.n, 3, '0')),
       CONCAT('/admin/orders?search=UI-SEED-ORDER-', LPAD(s.n, 3, '0')),
       'UI_SEED', s.n, MOD(s.n, 2),
       DATE_SUB(NOW(), INTERVAL MOD(s.n, 30) DAY)
FROM tmp_pk_ui_seed_seq s
JOIN users u ON u.email = CONCAT('ui.customer.', LPAD(s.n, 3, '0'), '@example.test')
WHERE NOT EXISTS (
    SELECT 1 FROM notifications n
    WHERE n.user_id = u.user_id
      AND n.title = CONCAT('UI-SEED thông báo ', LPAD(s.n, 3, '0'))
);

-- 12. 100 tệp thiết kế được dùng cho quy trình yêu cầu in 3D.
INSERT INTO custom_print_requests (
    user_id, design_file_id, shop_id, request_code, workflow_type, technology, material,
    infill_percent, layer_height_mm, quantity, color_option, post_processing_option,
    customer_note, dimensions_confirmed, estimated_total, quoted_amount, seller_response_note,
    production_days, quote_valid_until, status, order_id, submitted_at, quoted_at,
    accepted_at, declined_at, created_at, updated_at
)
SELECT u.user_id, df.design_file_id, sh.shop_id,
       CONCAT('UI-SEED-3DP-', LPAD(s.n, 3, '0')),
       CASE WHEN MOD(s.n, 2) = 0 THEN 'DIRECT_ORDER' ELSE 'QUOTE_REQUEST' END,
       CASE MOD(s.n, 3) WHEN 0 THEN 'SLA' ELSE 'FDM' END,
       CASE MOD(s.n, 3) WHEN 0 THEN 'Resin' WHEN 1 THEN 'PLA' ELSE 'PETG' END,
       10 + MOD(s.n, 90), 0.2000, 1 + MOD(s.n, 10),
       CASE WHEN MOD(s.n, 2) = 0 THEN 'Đen' ELSE 'Trắng' END,
       CASE WHEN MOD(s.n, 3) = 0 THEN 'Đánh bóng' ELSE 'Không' END,
       CONCAT('UI-SEED: yêu cầu in 3D minh họa số ', LPAD(s.n, 3, '0')),
       MOD(s.n, 2),
       90000 + s.n * 4500,
       CASE WHEN MOD(s.n, 5) = 0 THEN NULL ELSE 95000 + s.n * 4600 END,
       CASE WHEN MOD(s.n, 5) = 0 THEN NULL ELSE 'Báo giá kiểm thử, không phải báo giá thật.' END,
       2 + MOD(s.n, 8),
       DATE_ADD(NOW(), INTERVAL 7 DAY),
       CASE MOD(s.n, 5)
         WHEN 0 THEN 'PENDING_REVIEW'
         WHEN 1 THEN 'PENDING_REVIEW'
         WHEN 2 THEN 'READY_TO_ORDER'
         WHEN 3 THEN 'DRAFT'
         ELSE 'QUOTED'
       END,
       NULL,
       CASE WHEN MOD(s.n, 5) = 3 THEN NULL ELSE DATE_SUB(NOW(), INTERVAL MOD(s.n, 25) DAY) END,
       CASE WHEN MOD(s.n, 5) = 4 THEN DATE_SUB(NOW(), INTERVAL MOD(s.n, 10) DAY) ELSE NULL END,
       NULL, NULL,
       DATE_SUB(NOW(), INTERVAL MOD(s.n, 45) DAY),
       DATE_SUB(NOW(), INTERVAL MOD(s.n, 10) DAY)
FROM tmp_pk_ui_seed_seq s
JOIN users u ON u.email = CONCAT('ui.customer.', LPAD(s.n, 3, '0'), '@example.test')
JOIN design_files df ON df.user_id = u.user_id
    AND df.original_filename = CONCAT('ui-seed-design-', LPAD(s.n, 3, '0'), '.stl')
JOIN print_shops sh ON sh.slug = CONCAT('ui-seed-shop-', LPAD(s.n, 3, '0'))
WHERE NOT EXISTS (
    SELECT 1 FROM custom_print_requests r
    WHERE r.request_code = CONCAT('UI-SEED-3DP-', LPAD(s.n, 3, '0'))
);

INSERT INTO custom_print_request_history (
    custom_request_id, changed_by_user_id, old_status, new_status, note, created_at
)
SELECT r.custom_request_id, u.user_id, NULL, r.status,
       CONCAT('UI-SEED custom print history ', LPAD(s.n, 3, '0')),
       r.created_at
FROM tmp_pk_ui_seed_seq s
JOIN custom_print_requests r ON r.request_code = CONCAT('UI-SEED-3DP-', LPAD(s.n, 3, '0'))
JOIN users u ON u.email = CONCAT('ui.customer.', LPAD(s.n, 3, '0'), '@example.test')
WHERE NOT EXISTS (
    SELECT 1 FROM custom_print_request_history h
    WHERE h.custom_request_id = r.custom_request_id
      AND h.note = CONCAT('UI-SEED custom print history ', LPAD(s.n, 3, '0'))
);

INSERT INTO admin_custom_print_review_events (
    custom_request_id, admin_user_id, action, note, created_at
)
SELECT r.custom_request_id, @pk_ui_seed_admin_id, 'COMMENTED',
       CONCAT('UI-SEED admin review note ', LPAD(s.n, 3, '0')),
       DATE_SUB(NOW(), INTERVAL MOD(s.n, 12) DAY)
FROM tmp_pk_ui_seed_seq s
JOIN custom_print_requests r ON r.request_code = CONCAT('UI-SEED-3DP-', LPAD(s.n, 3, '0'))
WHERE NOT EXISTS (
    SELECT 1 FROM admin_custom_print_review_events e
    WHERE e.custom_request_id = r.custom_request_id
      AND e.note = CONCAT('UI-SEED admin review note ', LPAD(s.n, 3, '0'))
);

-- 13. 100 yêu cầu đổi/hủy đơn, trong đó có yêu cầu PENDING để test hàng đợi.
INSERT INTO order_change_requests (
    order_id, order_item_id, customer_user_id, shop_id, request_type, reason, payload_json,
    status, shop_response, price_delta, cancel_fee, customer_confirmed_at, expires_at,
    created_at, updated_at
)
SELECT o.order_id, oi.order_item_id, o.user_id, oi.shop_id,
       CASE WHEN MOD(s.n, 2) = 0 THEN 'CANCEL' ELSE 'CHANGE_ADDRESS' END,
       CONCAT('UI-SEED order change reason ', LPAD(s.n, 3, '0')),
       JSON_OBJECT('source', 'UI-SEED', 'sequence', s.n),
       CASE MOD(s.n, 5)
         WHEN 0 THEN 'PENDING'
         WHEN 1 THEN 'PENDING'
         WHEN 2 THEN 'APPROVED'
         WHEN 3 THEN 'REJECTED'
         ELSE 'CANCELLED'
       END,
       CASE WHEN MOD(s.n, 5) < 2 THEN NULL ELSE 'Phản hồi minh họa từ xưởng.' END,
       CASE WHEN MOD(s.n, 2) = 0 THEN 0 ELSE 25000 END,
       CASE WHEN MOD(s.n, 2) = 0 THEN 0 ELSE 10000 END,
       CASE WHEN MOD(s.n, 5) = 2 THEN DATE_SUB(NOW(), INTERVAL MOD(s.n, 10) DAY) ELSE NULL END,
       DATE_ADD(NOW(), INTERVAL 2 DAY),
       DATE_SUB(NOW(), INTERVAL MOD(s.n, 40) DAY),
       DATE_SUB(NOW(), INTERVAL MOD(s.n, 5) DAY)
FROM tmp_pk_ui_seed_seq s
JOIN orders o ON o.order_code = CONCAT('UI-SEED-ORDER-', LPAD(s.n, 3, '0'))
JOIN order_items oi ON oi.order_id = o.order_id
WHERE NOT EXISTS (
    SELECT 1 FROM order_change_requests r
    WHERE r.order_id = o.order_id
      AND r.reason = CONCAT('UI-SEED order change reason ', LPAD(s.n, 3, '0'))
);

INSERT INTO admin_order_change_review_events (
    change_request_id, admin_user_id, action, note, created_at
)
SELECT r.change_request_id, @pk_ui_seed_admin_id, 'COMMENTED',
       CONCAT('UI-SEED order change admin note ', LPAD(s.n, 3, '0')),
       DATE_SUB(NOW(), INTERVAL MOD(s.n, 10) DAY)
FROM tmp_pk_ui_seed_seq s
JOIN orders o ON o.order_code = CONCAT('UI-SEED-ORDER-', LPAD(s.n, 3, '0'))
JOIN order_change_requests r
  ON r.order_id = o.order_id
 AND r.reason = CONCAT('UI-SEED order change reason ', LPAD(s.n, 3, '0'))
WHERE NOT EXISTS (
    SELECT 1 FROM admin_order_change_review_events e
    WHERE e.change_request_id = r.change_request_id
      AND e.note = CONCAT('UI-SEED order change admin note ', LPAD(s.n, 3, '0'))
);

-- 14. 100 sự kiện xác minh xưởng.
INSERT INTO seller_verification_events (
    shop_id, admin_user_id, action, from_status, to_status, reason, created_at
)
SELECT sh.shop_id, @pk_ui_seed_admin_id,
       CASE sh.verification_status
         WHEN 'VERIFIED' THEN 'VERIFY'
         WHEN 'REJECTED' THEN 'REJECT'
         ELSE 'SUBMIT'
       END,
       'PENDING', sh.verification_status,
       CONCAT('UI-SEED seller verification ', LPAD(s.n, 3, '0')),
       DATE_SUB(NOW(), INTERVAL MOD(s.n, 45) DAY)
FROM tmp_pk_ui_seed_seq s
JOIN print_shops sh ON sh.slug = CONCAT('ui-seed-shop-', LPAD(s.n, 3, '0'))
WHERE NOT EXISTS (
    SELECT 1 FROM seller_verification_events e
    WHERE e.shop_id = sh.shop_id
      AND e.reason = CONCAT('UI-SEED seller verification ', LPAD(s.n, 3, '0'))
);

-- 15. 100 sự kiện đối soát thanh toán. Outcome OPEN giúp một phần hiện ở hàng đợi.
INSERT INTO admin_payment_reconciliation_events (
    payment_id, admin_user_id, outcome, note, provider_reference, created_at
)
SELECT p.payment_id, @pk_ui_seed_admin_id, 'OPEN',
       CONCAT('UI-SEED reconciliation event ', LPAD(s.n, 3, '0')),
       CONCAT('UI-SEED-RECON-', LPAD(s.n, 3, '0')),
       DATE_SUB(NOW(), INTERVAL MOD(s.n, 35) DAY)
FROM tmp_pk_ui_seed_seq s
JOIN orders o ON o.order_code = CONCAT('UI-SEED-ORDER-', LPAD(s.n, 3, '0'))
JOIN payments p ON p.order_id = o.order_id
WHERE NOT EXISTS (
    SELECT 1 FROM admin_payment_reconciliation_events e
    WHERE e.payment_id = p.payment_id
      AND e.provider_reference = CONCAT('UI-SEED-RECON-', LPAD(s.n, 3, '0'))
);

-- 16. 100 lịch sử đăng nhập và 100 sự kiện bảo mật để các trang tài khoản có dữ liệu.
INSERT INTO user_login_history (user_id, auth_provider, login_result, ip_address, user_agent, created_at)
SELECT u.user_id, 'local',
       CASE WHEN MOD(s.n, 9) = 0 THEN 'FAILED' ELSE 'SUCCESS' END,
       CONCAT('192.0.2.', 1 + MOD(s.n, 250)),
       CONCAT('PrintKraft UI Seed Agent ', LPAD(s.n, 3, '0')),
       DATE_SUB(NOW(), INTERVAL MOD(s.n, 45) DAY)
FROM tmp_pk_ui_seed_seq s
JOIN users u ON u.email = CONCAT('ui.customer.', LPAD(s.n, 3, '0'), '@example.test')
WHERE NOT EXISTS (
    SELECT 1 FROM user_login_history h
    WHERE h.user_id = u.user_id
      AND h.user_agent = CONCAT('PrintKraft UI Seed Agent ', LPAD(s.n, 3, '0'))
);

INSERT INTO user_security_events (user_id, event_type, description, created_at)
SELECT u.user_id,
       CASE WHEN MOD(s.n, 7) = 0 THEN 'PASSWORD_RESET' ELSE 'UI_SEED_TEST_EVENT' END,
       CONCAT('UI-SEED security event ', LPAD(s.n, 3, '0')),
       DATE_SUB(NOW(), INTERVAL MOD(s.n, 45) DAY)
FROM tmp_pk_ui_seed_seq s
JOIN users u ON u.email = CONCAT('ui.customer.', LPAD(s.n, 3, '0'), '@example.test')
WHERE NOT EXISTS (
    SELECT 1 FROM user_security_events e
    WHERE e.user_id = u.user_id
      AND e.description = CONCAT('UI-SEED security event ', LPAD(s.n, 3, '0'))
);

COMMIT;

-- Số lượng bản ghi UI-SEED sau khi chạy (kỳ vọng xấp xỉ 100/bảng).
SELECT 'users' AS table_name, COUNT(*) AS ui_seed_rows FROM users WHERE email LIKE 'ui.customer.%@example.test'
UNION ALL SELECT 'categories', COUNT(*) FROM categories WHERE slug LIKE 'ui-seed-category-%'
UNION ALL SELECT 'products', COUNT(*) FROM products WHERE slug LIKE 'ui-seed-product-%'
UNION ALL SELECT 'print_shops', COUNT(*) FROM print_shops WHERE slug LIKE 'ui-seed-shop-%'
UNION ALL SELECT 'shop_products', COUNT(*) FROM shop_products WHERE sku LIKE 'UI-SEED-SKU-%'
UNION ALL SELECT 'orders', COUNT(*) FROM orders WHERE order_code LIKE 'UI-SEED-ORDER-%'
UNION ALL SELECT 'order_items', COUNT(*) FROM order_items oi JOIN orders o ON o.order_id=oi.order_id WHERE o.order_code LIKE 'UI-SEED-ORDER-%'
UNION ALL SELECT 'payments', COUNT(*) FROM payments WHERE provider_transaction_ref LIKE 'UI-SEED-PAYMENT-%'
UNION ALL SELECT 'quote_requests', COUNT(*) FROM quote_requests WHERE company_name LIKE 'UI Seed Company %'
UNION ALL SELECT 'shipments', COUNT(*) FROM shipments WHERE tracking_number LIKE 'UISEEDTRACK%'
UNION ALL SELECT 'reviews', COUNT(*) FROM reviews r JOIN order_items oi ON oi.order_item_id=r.order_item_id JOIN orders o ON o.order_id=oi.order_id WHERE o.order_code LIKE 'UI-SEED-ORDER-%'
UNION ALL SELECT 'support_tickets', COUNT(*) FROM support_tickets WHERE ticket_code LIKE 'UI-SEED-TICKET-%'
UNION ALL SELECT 'custom_print_requests', COUNT(*) FROM custom_print_requests WHERE request_code LIKE 'UI-SEED-3DP-%'
UNION ALL SELECT 'order_change_requests', COUNT(*) FROM order_change_requests WHERE reason LIKE 'UI-SEED order change reason %';
