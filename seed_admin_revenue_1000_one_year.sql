-- =============================================================================
-- PrintKraft — 1,000 đơn hàng có doanh thu mẫu trải dài 365 ngày
-- =============================================================================
-- CHỈ DÙNG cho development/test. Không phải dữ liệu giao dịch thật.
-- Script KHÔNG DROP/TRUNCATE bảng thật, KHÔNG UPDATE/DELETE dữ liệu hiện có.
--
-- CẦN CHẠY TRƯỚC:
--   1) printkraft_database_safe.sql
--   2) seed_admin_dashboard_ui_100.sql
--
-- Script dùng 100 khách hàng, địa chỉ, sản phẩm, xưởng, chào bán và tệp thiết kế
-- do seed_admin_dashboard_ui_100.sql tạo. Nó thêm tối đa:
--   - 1.000 orders: ngày đặt trải đều từ CURDATE()-364 ngày đến hôm nay.
--   - 1.000 order_items: có giá trị dòng hàng để thống kê doanh số theo danh mục.
--   - 1.000 payments: status=PAID, paid_at khớp ngày đặt để biểu đồ thanh toán có dữ liệu.
--   - 1.000 order_status_history: mỗi đơn có một bản ghi lịch sử mẫu.
--
-- Mã định danh UI-SEED-REV giúp script chạy lại mà không nhân đôi đơn/thanh toán.
-- Để xem toàn bộ 12 tháng, chọn khoảng ngày từ ngày đầu đến ngày cuối rồi gom nhóm
-- theo THÁNG; nút "Từ đầu năm" chỉ hiển thị từ ngày 01/01 của năm hiện tại.
--
-- LƯU Ý: đây là doanh thu giả lập để kiểm tra biểu đồ, KPI và bảng quản trị.
-- Không chạy trên production và không dùng làm dữ liệu báo cáo kế toán thực tế.
-- =============================================================================

SET NAMES utf8mb4;
USE printkraft;

-- Tạo dãy số 1..1000 mà không phụ thuộc CTE hoặc cài đặt recursion của server.
DROP TEMPORARY TABLE IF EXISTS tmp_pk_revenue_seq;
CREATE TEMPORARY TABLE tmp_pk_revenue_seq (
    n INT NOT NULL PRIMARY KEY
) ENGINE=MEMORY;

INSERT INTO tmp_pk_revenue_seq (n)
SELECT hundreds.d * 100 + tens.d * 10 + ones.d + 1
FROM (SELECT 0 AS d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) AS ones
CROSS JOIN (SELECT 0 AS d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) AS tens
CROSS JOIN (SELECT 0 AS d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) AS hundreds;

-- Gắn 1.000 ngày với 100 bộ dữ liệu mẫu có sẵn. Mỗi ngày trong 365 ngày có
-- ít nhất 2 đơn và một số ngày có 3 đơn; ngày đặt không vượt quá hôm nay.
DROP TEMPORARY TABLE IF EXISTS tmp_pk_revenue_data;
CREATE TEMPORARY TABLE tmp_pk_revenue_data AS
SELECT
    s.n,
    MOD(s.n - 1, 100) + 1 AS seed_index,
    CONCAT('UI-SEED-REV-ORDER-', LPAD(s.n, 4, '0')) AS order_code,
    u.user_id,
    a.address_id,
    u.full_name AS recipient_name,
    u.phone AS recipient_phone,
    a.address_line,
    a.district,
    a.province,
    p.product_id,
    sh.shop_id,
    sp.shop_product_id,
    df.design_file_id,
    sp.unit_price,
    1 + MOD(s.n, 4) AS quantity,
    15000 + MOD(s.n, 5) * 5000 AS shipping_fee,
    DATE_ADD(
        DATE_SUB(CURDATE(), INTERVAL 364 DAY),
        INTERVAL MOD(s.n - 1, 365) DAY
    ) AS placed_at,
    CASE MOD(s.n, 4)
        WHEN 0 THEN 'Đã xác nhận'
        WHEN 1 THEN 'Đang xử lý'
        WHEN 2 THEN 'Đang giao hàng'
        ELSE 'Đã hoàn thành'
    END AS order_status
FROM tmp_pk_revenue_seq s
JOIN users u
  ON u.email = CONCAT('ui.customer.', LPAD(MOD(s.n - 1, 100) + 1, 3, '0'), '@example.test')
JOIN addresses a
  ON a.user_id = u.user_id
 AND a.address_line = CONCAT('UI Seed Address ', LPAD(MOD(s.n - 1, 100) + 1, 3, '0'))
JOIN products p
  ON p.slug = CONCAT('ui-seed-product-', LPAD(MOD(s.n - 1, 100) + 1, 3, '0'))
JOIN shop_products sp
  ON sp.sku = CONCAT('UI-SEED-SKU-', LPAD(MOD(s.n - 1, 100) + 1, 3, '0'))
 AND sp.product_id = p.product_id
JOIN print_shops sh
  ON sh.slug = CONCAT('ui-seed-shop-', LPAD(MOD(s.n - 1, 100) + 1, 3, '0'))
 AND sh.shop_id = sp.shop_id
JOIN design_files df
  ON df.user_id = u.user_id
 AND df.original_filename = CONCAT('ui-seed-design-', LPAD(MOD(s.n - 1, 100) + 1, 3, '0'), '.stl');

-- Bảo vệ khi base seed chưa được nạp hoặc thiếu một số quan hệ mẫu.
SELECT
    COUNT(*) AS prepared_revenue_rows,
    MIN(placed_at) AS first_order_date,
    MAX(placed_at) AS last_order_date
FROM tmp_pk_revenue_data;

SET @pk_ui_seed_admin_id := COALESCE(
    (SELECT user_id FROM users WHERE email = 'admin@printkraft.vn' AND UPPER(role) = 'ADMIN' LIMIT 1),
    (SELECT user_id FROM users WHERE UPPER(role) = 'ADMIN' LIMIT 1)
);

START TRANSACTION;

-- 1. Đơn hàng: tất cả là mẫu đã thanh toán để được tính vào doanh thu thống kê.
--    Status nghiệp vụ dùng giá trị tiếng Việt theo OrderStatus của ứng dụng.
INSERT INTO orders (
    order_code, user_id, address_id,
    recipient_name_snapshot, recipient_phone_snapshot, shipping_address_snapshot,
    subtotal, shipping_fee, total_amount, currency,
    status, payment_status, shipping_status,
    placed_at, completed_at, cancelled_at
)
SELECT
    r.order_code,
    r.user_id,
    r.address_id,
    r.recipient_name,
    r.recipient_phone,
    CONCAT(r.address_line, ', ', r.district, ', ', r.province),
    r.unit_price * r.quantity,
    r.shipping_fee,
    r.unit_price * r.quantity + r.shipping_fee,
    'VND',
    r.order_status,
    'PAID',
    CASE r.order_status
        WHEN 'Đang giao hàng' THEN 'Đang vận chuyển'
        WHEN 'Đã hoàn thành' THEN 'Đã giao'
        ELSE 'Chưa có'
    END,
    r.placed_at,
    CASE WHEN r.order_status = 'Đã hoàn thành' THEN r.placed_at ELSE NULL END,
    NULL
FROM tmp_pk_revenue_data r
WHERE NOT EXISTS (
    SELECT 1 FROM orders o WHERE o.order_code = r.order_code
);

-- 2. Một dòng hàng gắn đúng sản phẩm, xưởng và chào bán của mỗi đơn.
INSERT INTO order_items (
    order_id, product_id, shop_product_id, shop_id, design_file_id,
    product_name_snapshot, shop_name_snapshot, quantity, unit_price, line_total, status
)
SELECT
    o.order_id,
    r.product_id,
    r.shop_product_id,
    r.shop_id,
    r.design_file_id,
    p.product_name,
    sh.shop_name,
    r.quantity,
    r.unit_price,
    r.unit_price * r.quantity,
    o.status
FROM tmp_pk_revenue_data r
JOIN orders o ON o.order_code = r.order_code
JOIN products p ON p.product_id = r.product_id
JOIN print_shops sh ON sh.shop_id = r.shop_id
WHERE NOT EXISTS (
    SELECT 1 FROM order_items oi
    WHERE oi.order_id = o.order_id
      AND oi.shop_product_id = r.shop_product_id
);

-- 3. Một thanh toán PAID cho mỗi đơn. payment_method đa dạng để vẽ biểu đồ tròn.
--    Đây là giao dịch giả lập; không gọi cổng thanh toán thật.
INSERT INTO payments (
    order_id, payment_method, provider, provider_transaction_ref,
    provider_transaction_no, response_code, bank_code, card_type,
    checkout_url, amount, status, failure_reason, paid_at, created_at, updated_at
)
SELECT
    o.order_id,
    CASE MOD(r.n, 4)
        WHEN 0 THEN 'VNPAY'
        WHEN 1 THEN 'ZALOPAY'
        WHEN 2 THEN 'COD'
        ELSE 'BANK_TRANSFER'
    END,
    CASE MOD(r.n, 4)
        WHEN 0 THEN 'VNPAY'
        WHEN 1 THEN 'ZALOPAY'
        WHEN 2 THEN 'COD'
        ELSE 'BANK_TRANSFER'
    END,
    CONCAT('UI-SEED-REV-PAY-', LPAD(r.n, 4, '0')),
    CONCAT('UISEEDREVTX', LPAD(r.n, 8, '0')),
    '00',
    CASE WHEN MOD(r.n, 4) = 0 THEN 'NCB' ELSE NULL END,
    CASE WHEN MOD(r.n, 4) = 0 THEN 'DEMO' ELSE NULL END,
    NULL,
    o.total_amount,
    'PAID',
    NULL,
    o.placed_at,
    o.placed_at,
    o.placed_at
FROM tmp_pk_revenue_data r
JOIN orders o ON o.order_code = r.order_code
WHERE NOT EXISTS (
    SELECT 1 FROM payments p
    WHERE p.provider_transaction_ref = CONCAT('UI-SEED-REV-PAY-', LPAD(r.n, 4, '0'))
);

-- 4. Lịch sử trạng thái phục vụ trang chi tiết đơn hàng.
INSERT INTO order_status_history (
    order_id, changed_by_user_id, old_status, new_status, note, changed_at
)
SELECT
    o.order_id,
    @pk_ui_seed_admin_id,
    'UI-SEED_CREATED',
    o.status,
    CONCAT('UI-SEED revenue history ', LPAD(r.n, 4, '0')),
    o.placed_at
FROM tmp_pk_revenue_data r
JOIN orders o ON o.order_code = r.order_code
WHERE NOT EXISTS (
    SELECT 1 FROM order_status_history h
    WHERE h.order_id = o.order_id
      AND h.note = CONCAT('UI-SEED revenue history ', LPAD(r.n, 4, '0'))
);

COMMIT;

-- Kiểm tra số đơn, số dòng hàng, số thanh toán và khoảng ngày thực tế.
SELECT
    COUNT(*) AS revenue_sample_orders,
    MIN(placed_at) AS first_order_date,
    MAX(placed_at) AS last_order_date,
    SUM(total_amount) AS simulated_revenue_vnd
FROM orders
WHERE order_code LIKE 'UI-SEED-REV-ORDER-%';

SELECT 'orders' AS table_name, COUNT(*) AS sample_rows
FROM orders WHERE order_code LIKE 'UI-SEED-REV-ORDER-%'
UNION ALL
SELECT 'order_items', COUNT(*)
FROM order_items oi
JOIN orders o ON o.order_id = oi.order_id
WHERE o.order_code LIKE 'UI-SEED-REV-ORDER-%'
UNION ALL
SELECT 'payments', COUNT(*)
FROM payments WHERE provider_transaction_ref LIKE 'UI-SEED-REV-PAY-%'
UNION ALL
SELECT 'order_status_history', COUNT(*)
FROM order_status_history
WHERE note LIKE 'UI-SEED revenue history %';

DROP TEMPORARY TABLE IF EXISTS tmp_pk_revenue_data;
DROP TEMPORARY TABLE IF EXISTS tmp_pk_revenue_seq;
