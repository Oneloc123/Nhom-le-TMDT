-- PrintKraft: dữ liệu demo cho trang chi tiết nhà cung cấp id=13.
-- Chỉ dùng local/development. Các nhận xét được đánh dấu DEMO, không phải đánh giá khách hàng thật.
-- Chạy trong Navicat với database printkraft đã có schema printkraft_database_safe.sql.
-- Script KHÔNG tạo nhà cung cấp mới và KHÔNG ghi đè thông tin đã có.
-- Nếu shop id không phải 13 trong database của bạn, sửa @target_shop_id trước khi chạy.

SET NAMES utf8mb4;
USE `printkraft`;
SET @target_shop_id := 13;

START TRANSACTION;

-- Nếu id=13 chưa tồn tại, tạo một hồ sơ demo tại đúng id để các liên kết thử nghiệm có đích.
-- Dùng người dùng ACTIVE đầu tiên làm owner để không cần tạo tài khoản đăng nhập mới.
INSERT INTO `print_shops`
  (`shop_id`, `owner_user_id`, `shop_name`, `slug`, `description`, `contact_phone`,
   `contact_email`, `address_line`, `province`, `district`, `search_keywords`,
   `capabilities`, `accepted_file_formats`, `max_build_dimensions`, `operating_hours`,
   `estimated_lead_days`, `service_notes`, `average_rating`, `verification_status`, `status`, `created_at`)
SELECT 13, u.`user_id`, 'PrintKraft Demo Supplier 13', 'demo-supplier-id-13',
       'Hồ sơ minh họa để kiểm thử trang chi tiết nhà cung cấp. Không phải doanh nghiệp thật.',
       '0900000013', 'supplier13@example.test', 'Địa chỉ mẫu - chỉ dùng kiểm thử',
       'TP. Hồ Chí Minh', 'Khu vực mẫu', 'in 3D FDM SLA resin prototype',
       'FDM, SLA, prototype, mô hình minh họa', 'STL, OBJ, 3MF',
       '220x220x250 mm (mẫu)', '09:00-17:00 (mẫu)', 5,
       'DỮ LIỆU DEMO: vui lòng xác nhận giá, thời gian và năng lực thực tế.',
       0, 'VERIFIED', 'ACTIVE', NOW()
FROM `users` u
WHERE u.`status` = 'ACTIVE'
  AND NOT EXISTS (SELECT 1 FROM `print_shops` s WHERE s.`shop_id` = @target_shop_id)
  AND NOT EXISTS (SELECT 1 FROM `print_shops` s2 WHERE s2.`slug` = 'demo-supplier-id-13')
ORDER BY u.`user_id`
LIMIT 1;

-- 1) Bổ sung thông tin còn thiếu cho hồ sơ nhà cung cấp.
-- Trạng thái đã có sẽ được giữ nguyên; thông tin thật không bị thay thế.
UPDATE `print_shops`
SET
  `description` = COALESCE(NULLIF(TRIM(`description`), ''), 'Hồ sơ minh họa để kiểm thử trang nhà cung cấp PrintKraft. Vui lòng xác nhận thông tin và năng lực trước khi đặt hàng.'),
  `contact_phone` = COALESCE(NULLIF(TRIM(`contact_phone`), ''), '0900000013'),
  `contact_email` = COALESCE(NULLIF(TRIM(`contact_email`), ''), 'supplier13@example.test'),
  `address_line` = COALESCE(NULLIF(TRIM(`address_line`), ''), 'Địa chỉ mẫu - chỉ dùng kiểm thử'),
  `province` = COALESCE(NULLIF(TRIM(`province`), ''), 'TP. Hồ Chí Minh'),
  `district` = COALESCE(NULLIF(TRIM(`district`), ''), 'Khu vực mẫu'),
  `search_keywords` = COALESCE(NULLIF(TRIM(`search_keywords`), ''), 'in 3D FDM SLA resin prototype'),
  `capabilities` = COALESCE(NULLIF(TRIM(`capabilities`), ''), 'FDM, SLA, prototype, mô hình minh họa'),
  `accepted_file_formats` = COALESCE(NULLIF(TRIM(`accepted_file_formats`), ''), 'STL, OBJ, 3MF'),
  `max_build_dimensions` = COALESCE(NULLIF(TRIM(`max_build_dimensions`), ''), '220x220x250 mm (mẫu)'),
  `operating_hours` = COALESCE(NULLIF(TRIM(`operating_hours`), ''), '09:00-17:00 (mẫu)'),
  `estimated_lead_days` = COALESCE(`estimated_lead_days`, 5),
  `service_notes` = COALESCE(NULLIF(TRIM(`service_notes`), ''), 'DỮ LIỆU DEMO: vui lòng xác nhận giá, thời gian và năng lực thực tế.')
WHERE `shop_id` = @target_shop_id;

-- 2) Thêm tối đa ba đánh giá demo công khai nếu có người dùng hiện hữu.
-- order_item_id để NULL nhằm không giả mạo liên kết với đơn hàng thật.
-- ReviewDao chỉ hiển thị is_published=1; NULL order_item_id sẽ không được gắn nhãn người mua đã xác minh.
INSERT INTO `reviews`
  (`order_item_id`, `user_id`, `product_id`, `shop_id`, `rating`, `review_text`, `is_published`, `created_at`)
SELECT
  NULL,
  u.`user_id`,
  (SELECT sp.`product_id`
   FROM `shop_products` sp
   WHERE sp.`shop_id` = @target_shop_id AND sp.`is_active` = 1
   ORDER BY sp.`shop_product_id` LIMIT 1),
  @target_shop_id,
  5,
  'DEMO-SUPPLIER-13-01: Đánh giá minh họa giao diện, không phải phản hồi khách hàng thật.',
  1,
  DATE_SUB(NOW(), INTERVAL 3 DAY)
FROM `users` u
WHERE u.`user_id` = (SELECT x.`user_id` FROM (SELECT `user_id` FROM `users` WHERE `status` = 'ACTIVE' ORDER BY `user_id` LIMIT 1) x)
  AND EXISTS (SELECT 1 FROM `print_shops` s WHERE s.`shop_id` = @target_shop_id AND (s.`status` IS NULL OR s.`status` NOT IN ('INACTIVE','BLOCKED')) AND (s.`verification_status` = 'VERIFIED' OR s.`verification_status` IS NULL))
  AND NOT EXISTS (SELECT 1 FROM `reviews` r WHERE r.`shop_id` = @target_shop_id AND r.`review_text` LIKE 'DEMO-SUPPLIER-13-01:%');

INSERT INTO `reviews`
  (`order_item_id`, `user_id`, `product_id`, `shop_id`, `rating`, `review_text`, `is_published`, `created_at`)
SELECT
  NULL,
  u.`user_id`,
  (SELECT sp.`product_id`
   FROM `shop_products` sp
   WHERE sp.`shop_id` = @target_shop_id AND sp.`is_active` = 1
   ORDER BY sp.`shop_product_id` LIMIT 1),
  @target_shop_id,
  4,
  'DEMO-SUPPLIER-13-02: Đánh giá minh họa về quy trình trao đổi, không phải phản hồi khách hàng thật.',
  1,
  DATE_SUB(NOW(), INTERVAL 7 DAY)
FROM `users` u
WHERE u.`user_id` = (SELECT x.`user_id` FROM (SELECT `user_id` FROM `users` WHERE `status` = 'ACTIVE' ORDER BY `user_id` LIMIT 1 OFFSET 1) x)
  AND EXISTS (SELECT 1 FROM `print_shops` s WHERE s.`shop_id` = @target_shop_id AND (s.`status` IS NULL OR s.`status` NOT IN ('INACTIVE','BLOCKED')) AND (s.`verification_status` = 'VERIFIED' OR s.`verification_status` IS NULL))
  AND NOT EXISTS (SELECT 1 FROM `reviews` r WHERE r.`shop_id` = @target_shop_id AND r.`review_text` LIKE 'DEMO-SUPPLIER-13-02:%');

INSERT INTO `reviews`
  (`order_item_id`, `user_id`, `product_id`, `shop_id`, `rating`, `review_text`, `is_published`, `created_at`)
SELECT
  NULL,
  u.`user_id`,
  (SELECT sp.`product_id`
   FROM `shop_products` sp
   WHERE sp.`shop_id` = @target_shop_id AND sp.`is_active` = 1
   ORDER BY sp.`shop_product_id` LIMIT 1),
  @target_shop_id,
  5,
  'DEMO-SUPPLIER-13-03: Nội dung mẫu để kiểm thử danh sách đánh giá, không phải phản hồi khách hàng thật.',
  1,
  DATE_SUB(NOW(), INTERVAL 14 DAY)
FROM `users` u
WHERE u.`user_id` = (SELECT x.`user_id` FROM (SELECT `user_id` FROM `users` WHERE `status` = 'ACTIVE' ORDER BY `user_id` LIMIT 1 OFFSET 2) x)
  AND EXISTS (SELECT 1 FROM `print_shops` s WHERE s.`shop_id` = @target_shop_id AND (s.`status` IS NULL OR s.`status` NOT IN ('INACTIVE','BLOCKED')) AND (s.`verification_status` = 'VERIFIED' OR s.`verification_status` IS NULL))
  AND NOT EXISTS (SELECT 1 FROM `reviews` r WHERE r.`shop_id` = @target_shop_id AND r.`review_text` LIKE 'DEMO-SUPPLIER-13-03:%');

-- Cập nhật cache điểm trung bình ở print_shops khi đã có đánh giá công khai.
UPDATE `print_shops` s
SET s.`average_rating` = (
  SELECT ROUND(AVG(r.`rating`), 2)
  FROM `reviews` r
  WHERE r.`shop_id` = s.`shop_id` AND r.`is_published` = 1
)
WHERE s.`shop_id` = @target_shop_id
  AND EXISTS (SELECT 1 FROM `reviews` r2 WHERE r2.`shop_id` = @target_shop_id AND r2.`is_published` = 1);

COMMIT;

-- 3) Kiểm tra kết quả.
SELECT
  s.`shop_id`, s.`shop_name`, s.`verification_status`, s.`status`,
  s.`contact_phone`, s.`contact_email`,
  (SELECT COUNT(*) FROM `reviews` r WHERE r.`shop_id` = s.`shop_id` AND r.`is_published` = 1) AS published_review_count,
  (SELECT ROUND(AVG(r.`rating`), 1) FROM `reviews` r WHERE r.`shop_id` = s.`shop_id` AND r.`is_published` = 1) AS published_average_rating
FROM `print_shops` s
WHERE s.`shop_id` = @target_shop_id;

-- Nếu shop id=13 là hồ sơ thật đang ở trạng thái PENDING, không tự động xác minh hồ sơ đó.
-- Chỉ trong database local/demo, bạn có thể chủ động chạy:
-- UPDATE `print_shops` SET `verification_status`='VERIFIED', `status`='ACTIVE' WHERE `shop_id`=13;
-- Nếu truy vấn cuối không trả hàng, shop id=13 không có trong database này:
--   SELECT shop_id, shop_name, verification_status, status FROM print_shops ORDER BY shop_id;
-- Sửa @target_shop_id thành shop_id thực tế rồi chạy lại.
