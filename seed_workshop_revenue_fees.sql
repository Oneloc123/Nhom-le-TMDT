-- =============================================================================
-- PrintKraft: dữ liệu mẫu cho trang Doanh thu xưởng và Phí nền tảng
-- =============================================================================
-- CHỈ DÙNG TRÊN DEVELOPMENT / TEST; KHÔNG chạy trên production.
-- Script chỉ tác động tới đơn và xưởng có mã UI-SEED, không xóa dữ liệu.
-- Chạy trước: printkraft_database_safe.sql, seed_admin_dashboard_ui_100.sql,
-- seed_admin_revenue_1000_one_year.sql (khuyến nghị), rồi migrate_20261125_platform_finance.sql.
-- Tỷ lệ phí được lấy từ platform_finance_settings; mặc định của dự án là 10% trên
-- giá trị sản phẩm, KHÔNG gồm phí vận chuyển. Không tạo giao dịch/cổng thanh toán thật.
-- =============================================================================

SET NAMES utf8mb4;
USE printkraft;

SET @pk_fee_rate := COALESCE(
    (SELECT commission_rate FROM platform_finance_settings WHERE setting_id = 1), 10.00
);

START TRANSACTION;

-- Chuẩn hóa riêng các đơn fixture đã có payment PAID để khớp quy tắc tài chính của ứng dụng.
UPDATE orders o
JOIN (
    SELECT p.order_id, MAX(p.payment_id) AS paid_payment_id
    FROM payments p
    WHERE UPPER(COALESCE(p.status, '')) = 'PAID'
    GROUP BY p.order_id
) paid ON paid.order_id = o.order_id
SET o.payment_status = 'Đã thanh toán'
WHERE (o.order_code LIKE 'UI-SEED-ORDER-%'
    OR o.order_code LIKE 'UI-SEED-REV-ORDER-%')
  AND COALESCE(o.payment_status, '') <> 'Đã thanh toán';

-- Tạo snapshot phân bổ theo từng dòng hàng. order_item_id đã có allocation thì giữ nguyên.
INSERT INTO order_financial_allocations (
    order_id, order_item_id, shop_id, payment_id, currency,
    gross_product_amount, commission_rate_snapshot, commission_amount,
    seller_net_amount, allocation_status, created_at
)
SELECT oi.order_id, oi.order_item_id, oi.shop_id, p.payment_id, COALESCE(o.currency, 'VND'),
       oi.line_total, @pk_fee_rate,
       ROUND(oi.line_total * @pk_fee_rate / 100, 2),
       oi.line_total - ROUND(oi.line_total * @pk_fee_rate / 100, 2),
       'AVAILABLE', COALESCE(o.placed_at, p.paid_at, p.created_at, NOW())
FROM order_items oi
JOIN orders o ON o.order_id = oi.order_id
JOIN print_shops sh ON sh.shop_id = oi.shop_id AND sh.slug LIKE 'ui-seed-shop-%'
JOIN payments p ON p.payment_id = (
    SELECT MAX(p2.payment_id)
    FROM payments p2
    WHERE p2.order_id = o.order_id AND UPPER(COALESCE(p2.status, '')) = 'PAID'
)
WHERE (o.order_code LIKE 'UI-SEED-ORDER-%'
    OR o.order_code LIKE 'UI-SEED-REV-ORDER-%')
  AND o.payment_status = 'Đã thanh toán'
  AND p.status = 'PAID'
  AND oi.line_total > 0
ON DUPLICATE KEY UPDATE order_item_id = VALUES(order_item_id);

-- Tạo 1 kỳ READY cho tháng dương lịch gần nhất đã kết thúc, lấy các khoản đang chờ
-- quyết toán của đơn đã hoàn thành tính lũy kế đến cuối tháng. Đây là fixture, chưa chi tiền.
SET @pk_period_start := DATE_SUB(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 1 MONTH);
SET @pk_period_end := LAST_DAY(@pk_period_start);

INSERT INTO seller_monthly_settlements (
    shop_id, period_start, period_end, currency,
    gross_product_amount, commission_amount, seller_net_amount,
    refund_amount, commission_reversal_amount, seller_recovery_amount,
    status, payout_reference, paid_by_user_id, paid_at, note, created_at
)
SELECT totals.shop_id, @pk_period_start, @pk_period_end, totals.currency,
       totals.gross_amount, totals.fee_amount, totals.net_amount,
       0, 0, 0, 'READY', NULL, NULL, NULL,
       'UI-SEED finance fixture: ready settlement', NOW()
FROM (
    SELECT a.shop_id, a.currency,
           SUM(a.gross_product_amount) AS gross_amount,
           SUM(a.commission_amount) AS fee_amount,
           SUM(a.seller_net_amount) AS net_amount
    FROM order_financial_allocations a
    JOIN orders o ON o.order_id = a.order_id
    JOIN print_shops sh ON sh.shop_id = a.shop_id AND sh.slug LIKE 'ui-seed-shop-%'
    WHERE a.allocation_status = 'AVAILABLE' AND a.settlement_id IS NULL
      AND o.status = 'Đã hoàn thành'
      AND (o.order_code LIKE 'UI-SEED-ORDER-%'
        OR o.order_code LIKE 'UI-SEED-REV-ORDER-%')
      AND a.created_at < DATE_ADD(@pk_period_end, INTERVAL 1 DAY)
    GROUP BY a.shop_id, a.currency
    HAVING SUM(a.seller_net_amount) > 0
) totals
WHERE NOT EXISTS (
    SELECT 1 FROM seller_monthly_settlements existing
    WHERE existing.shop_id = totals.shop_id
      AND existing.period_start = @pk_period_start
      AND existing.period_end = @pk_period_end
      AND existing.currency = totals.currency
);

-- Chỉ gắn allocation với kỳ fixture của file này; không thay đổi kỳ nguồn khác.
UPDATE order_financial_allocations a
JOIN orders o ON o.order_id = a.order_id
JOIN print_shops sh ON sh.shop_id = a.shop_id AND sh.slug LIKE 'ui-seed-shop-%'
JOIN seller_monthly_settlements st
  ON st.shop_id = a.shop_id AND st.currency = a.currency
 AND st.period_start = @pk_period_start AND st.period_end = @pk_period_end
 AND st.status = 'READY'
 AND st.note = 'UI-SEED finance fixture: ready settlement'
SET a.allocation_status = 'SETTLED', a.settlement_id = st.settlement_id,
    a.settled_at = COALESCE(a.settled_at, NOW())
WHERE a.allocation_status = 'AVAILABLE' AND a.settlement_id IS NULL
  AND o.status = 'Đã hoàn thành'
  AND (o.order_code LIKE 'UI-SEED-ORDER-%'
    OR o.order_code LIKE 'UI-SEED-REV-ORDER-%')
  AND a.created_at < DATE_ADD(@pk_period_end, INTERVAL 1 DAY);

COMMIT;

-- Kiểm tra tổng doanh thu/phí/thực nhận theo xưởng (tối đa 50 xưởng mẫu).
SELECT sh.shop_id, sh.shop_name, sh.owner_user_id, u.email AS owner_email,
       COUNT(a.allocation_id) AS allocation_count,
       COALESCE(SUM(a.gross_product_amount), 0) AS gross_product_amount,
       COALESCE(SUM(a.commission_amount), 0) AS platform_fee_amount,
       COALESCE(SUM(a.seller_net_amount), 0) AS seller_net_amount,
       COALESCE(SUM(CASE WHEN a.allocation_status = 'AVAILABLE'
                         THEN a.seller_net_amount ELSE 0 END), 0) AS available_revenue,
       COALESCE(SUM(CASE WHEN a.allocation_status = 'SETTLED'
                         THEN a.seller_net_amount ELSE 0 END), 0) AS allocated_to_settlement
FROM print_shops sh
JOIN users u ON u.user_id = sh.owner_user_id
LEFT JOIN order_financial_allocations a ON a.shop_id = sh.shop_id
LEFT JOIN orders o ON o.order_id = a.order_id
WHERE sh.slug LIKE 'ui-seed-shop-%'
  AND (o.order_code LIKE 'UI-SEED-ORDER-%'
    OR o.order_code LIKE 'UI-SEED-REV-ORDER-%'
    OR o.order_code IS NULL)
GROUP BY sh.shop_id, sh.shop_name, sh.owner_user_id, u.email
HAVING COUNT(a.allocation_id) > 0
ORDER BY gross_product_amount DESC
LIMIT 50;

-- Các kỳ quyết toán fixture dùng trên trang Doanh thu xưởng.
SELECT st.settlement_id, st.shop_id, sh.shop_name, st.period_start, st.period_end,
       st.gross_product_amount, st.commission_amount, st.seller_net_amount,
       st.status, st.payout_reference
FROM seller_monthly_settlements st
JOIN print_shops sh ON sh.shop_id = st.shop_id
WHERE sh.slug LIKE 'ui-seed-shop-%'
  AND st.note = 'UI-SEED finance fixture: ready settlement'
ORDER BY st.period_end DESC, st.shop_id
LIMIT 50;

-- Các dòng gần nhất tương ứng bảng Chi tiết phí nền tảng.
SELECT sh.shop_name, o.order_code,
       a.gross_product_amount AS product_amount,
       a.commission_rate_snapshot AS fee_rate_percent,
       a.commission_amount AS platform_fee,
       a.seller_net_amount AS workshop_net_amount,
       a.allocation_status, a.created_at
FROM order_financial_allocations a
JOIN orders o ON o.order_id = a.order_id
JOIN print_shops sh ON sh.shop_id = a.shop_id
WHERE sh.slug LIKE 'ui-seed-shop-%'
  AND (o.order_code LIKE 'UI-SEED-ORDER-%'
    OR o.order_code LIKE 'UI-SEED-REV-ORDER-%')
ORDER BY a.created_at DESC, a.allocation_id DESC
LIMIT 50;
