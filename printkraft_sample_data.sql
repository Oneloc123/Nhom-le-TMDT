-- =============================================================================
-- PrintKraft — dữ liệu demo đầy đủ cho schema hiện tại (development/test only)
-- =============================================================================
-- Chạy sau printkraft_database_safe.sql trên database development `printkraft`.
-- Script chỉ INSERT các hàng có dấu nhận diện demo; không UPDATE hoặc DELETE dữ liệu
-- hiện có. Có thể chạy lại: mỗi phần bỏ qua email/slug/SKU/mã đơn/khóa mẫu đã có.
-- Script có dữ liệu thanh toán và đánh giá giả lập để kiểm tra giao diện; KHÔNG phải
-- giao dịch, đơn hàng, đánh giá hay tệp thật. Không chạy trên production.
--
-- Tài khoản demo:
--   admin@printkraft.vn / admin123 (ADMIN)
--   minhtuan@gmail.com / 123456 (CUSTOMER)
--   workshop01@example.test ... workshop05@example.test / Print3D-Demo-2026!
-- Mật khẩu lưu bằng PBKDF2; nếu email đã tồn tại, script giữ nguyên tài khoản đó.
-- Các shop mới có 4 VERIFIED và 1 PENDING để thử cả marketplace lẫn duyệt Admin.
-- Một đơn COD được mô phỏng ở trạng thái hoàn tất; bản ghi payment vẫn PENDING,
-- không ghi nhận thanh toán online thành công. Mọi dòng đều thuộc dữ liệu demo.
--
-- Navicat: xác nhận database đang chọn là `printkraft`, mở file này và chạy toàn bộ.
-- Nếu có lỗi trước COMMIT, dừng và chạy ROLLBACK trong cùng kết nối.
-- =============================================================================

SET NAMES utf8mb4;
USE `printkraft`;
START TRANSACTION;

-- 1. Tài khoản mẫu (2 khách/Admin hiện hữu + 5 chủ xưởng demo; không cấp role riêng).
INSERT INTO `users` (`full_name`, `email`, `phone`, `password_hash`, `role`, `status`, `created_at`)
SELECT 'Quản trị viên', 'admin@printkraft.vn', '0901000001',
       'pbkdf2$210000$zrfD+LrRM1ld0BMdRCtLXQ==$4VtK1qg++9Poqf5RVyQvj70i3WiT6Fl6m7M/5FnEO6o=',
       'ADMIN', 'ACTIVE', '2026-10-01 09:00:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `users` WHERE `email` = 'admin@printkraft.vn');

INSERT INTO `users` (`full_name`, `email`, `phone`, `password_hash`, `role`, `status`, `created_at`)
SELECT 'Nguyễn Minh Tuấn', 'minhtuan@gmail.com', '0912345678',
       'pbkdf2$210000$YJN3Plki0bYpOHxSP2kiqA==$T6Dzel5zx/TumJomdpj4VF3dEHMvWLEKmlabGrZElQk=',
       'CUSTOMER', 'ACTIVE', '2026-10-01 09:05:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `users` WHERE `email` = 'minhtuan@gmail.com');

INSERT INTO `users` (`full_name`, `email`, `phone`, `password_hash`, `role`, `status`, `created_at`)
SELECT 'Nguyễn Minh Tân', 'workshop01@example.test', '0903100001',
       'pbkdf2$210000$OQuEtigHCqxAIhHEvSyeDQ==$qkjoit2Kw/s//PFsODlvwdSmV5XRzL1HyMc9ku9gpyY=',
       'CUSTOMER', 'ACTIVE', '2026-10-01 09:10:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `users` WHERE `email` = 'workshop01@example.test');

INSERT INTO `users` (`full_name`, `email`, `phone`, `password_hash`, `role`, `status`, `created_at`)
SELECT 'Trần Ngọc Anh', 'workshop02@example.test', '0903100002',
       'pbkdf2$210000$Re8Kv6tNitxfioaGf4PswQ==$KQq4AdrcBCY7ieAWFCDl2+SJBpvZVIf9mguUCl1y+yM=',
       'CUSTOMER', 'ACTIVE', '2026-10-01 09:11:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `users` WHERE `email` = 'workshop02@example.test');

INSERT INTO `users` (`full_name`, `email`, `phone`, `password_hash`, `role`, `status`, `created_at`)
SELECT 'Lê Quốc Bảo', 'workshop03@example.test', '0903100003',
       'pbkdf2$210000$VFxae8GSZohboK8B0bbKgw==$jpuVNvPZytMZOm8WzL1ZQy0vfOEXqWgdBFIWKy3PFaE=',
       'CUSTOMER', 'ACTIVE', '2026-10-01 09:12:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `users` WHERE `email` = 'workshop03@example.test');

INSERT INTO `users` (`full_name`, `email`, `phone`, `password_hash`, `role`, `status`, `created_at`)
SELECT 'Phạm Hoàng Long', 'workshop04@example.test', '0903100004',
       'pbkdf2$210000$x5fW2DzhGgFnCKGtdCXHXQ==$KmaXa6h9U8nLUSFKS0LWWqfSHVp9t2r1OlZYgi9GwgQ=',
       'CUSTOMER', 'ACTIVE', '2026-10-01 09:13:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `users` WHERE `email` = 'workshop04@example.test');

INSERT INTO `users` (`full_name`, `email`, `phone`, `password_hash`, `role`, `status`, `created_at`)
SELECT 'Võ Thu Hà', 'workshop05@example.test', '0903100005',
       'pbkdf2$210000$dRjCjcL0247Z/n89VZS6QA==$eDwEsVnUZRSDcWHIIyN0uDHOZve8IVKOqj4SuI0hGYs=',
       'CUSTOMER', 'ACTIVE', '2026-10-01 09:14:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `users` WHERE `email` = 'workshop05@example.test');

-- 2. Danh mục 3D.
INSERT INTO `categories` (`parent_id`, `category_name`, `slug`, `is_active`, `display_order`)
SELECT NULL, 'Sản phẩm in 3D', 'sample-3d-root', 1, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `categories` WHERE `slug` = 'sample-3d-root');

INSERT INTO `categories` (`parent_id`, `category_name`, `slug`, `is_active`, `display_order`)
SELECT (SELECT `category_id` FROM `categories` WHERE `slug` = 'sample-3d-root' LIMIT 1),
       'Phụ kiện FDM', 'sample-3d-fdm', 1, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `categories` WHERE `slug` = 'sample-3d-fdm');

INSERT INTO `categories` (`parent_id`, `category_name`, `slug`, `is_active`, `display_order`)
SELECT (SELECT `category_id` FROM `categories` WHERE `slug` = 'sample-3d-root' LIMIT 1),
       'Sản phẩm resin', 'sample-3d-resin', 1, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `categories` WHERE `slug` = 'sample-3d-resin');

INSERT INTO `categories` (`parent_id`, `category_name`, `slug`, `is_active`, `display_order`)
SELECT (SELECT `category_id` FROM `categories` WHERE `slug` = 'sample-3d-root' LIMIT 1),
       'Miniature và mô hình', 'sample-3d-miniature', 1, 3
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `categories` WHERE `slug` = 'sample-3d-miniature');

INSERT INTO `categories` (`parent_id`, `category_name`, `slug`, `is_active`, `display_order`)
SELECT (SELECT `category_id` FROM `categories` WHERE `slug` = 'sample-3d-root' LIMIT 1),
       'Prototype kỹ thuật', 'sample-3d-prototype', 1, 4
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `categories` WHERE `slug` = 'sample-3d-prototype');

INSERT INTO `categories` (`parent_id`, `category_name`, `slug`, `is_active`, `display_order`)
SELECT (SELECT `category_id` FROM `categories` WHERE `slug` = 'sample-3d-root' LIMIT 1),
       'Đồ gia dụng và trang trí', 'sample-3d-home', 1, 5
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `categories` WHERE `slug` = 'sample-3d-home');

INSERT INTO `categories` (`parent_id`, `category_name`, `slug`, `is_active`, `display_order`)
SELECT (SELECT `category_id` FROM `categories` WHERE `slug` = 'sample-3d-root' LIMIT 1),
       'Đồ dùng cá nhân', 'sample-3d-personal', 1, 6
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `categories` WHERE `slug` = 'sample-3d-personal');

-- 3. Sản phẩm mẫu: có APPROVED, PENDING_REVIEW, REJECTED và một sản phẩm ẩn.
-- is_active được nạp riêng và không bị cập nhật bởi trạng thái kiểm duyệt.
INSERT INTO `products`
  (`category_id`, `product_name`, `slug`, `description`, `brand`, `base_unit`,
   `is_customizable`, `is_active`, `view_count`, `moderation_status`, `rejection_reason`,
   `reviewed_by_user_id`, `reviewed_at`, `search_keywords`, `created_at`, `updated_at`)
SELECT c.`category_id`, 'Đế điện thoại mô-đun', 'demo-3d-phone-stand',
       'Đế điện thoại in FDM có khe đi dây; dữ liệu minh họa.', 'Demo Print3D', 'cái',
       1, 1, 24, 'APPROVED', NULL,
       (SELECT `user_id` FROM `users` WHERE `email` = 'admin@printkraft.vn' AND `role` = 'ADMIN' AND `status` = 'ACTIVE' LIMIT 1),
       '2026-10-02 10:00:00', 'đế điện thoại FDM PLA', '2026-10-01 10:00:00', '2026-10-02 10:00:00'
FROM `categories` c WHERE c.`slug` = 'sample-3d-fdm'
  AND NOT EXISTS (SELECT 1 FROM `products` WHERE `slug` = 'demo-3d-phone-stand');

INSERT INTO `products`
  (`category_id`, `product_name`, `slug`, `description`, `brand`, `base_unit`,
   `is_customizable`, `is_active`, `view_count`, `moderation_status`, `rejection_reason`,
   `reviewed_by_user_id`, `reviewed_at`, `search_keywords`, `created_at`, `updated_at`)
SELECT c.`category_id`, 'Rồng khớp nối mini', 'demo-3d-articulated-dragon',
       'Mô hình rồng khớp nối mẫu, có thể chọn màu filament.', 'Demo Print3D', 'cái',
       1, 1, 18, 'APPROVED', NULL,
       (SELECT `user_id` FROM `users` WHERE `email` = 'admin@printkraft.vn' AND `role` = 'ADMIN' AND `status` = 'ACTIVE' LIMIT 1),
       '2026-10-02 10:00:00', 'rồng khớp nối mô hình FDM', '2026-10-01 10:05:00', '2026-10-02 10:00:00'
FROM `categories` c WHERE c.`slug` = 'sample-3d-miniature'
  AND NOT EXISTS (SELECT 1 FROM `products` WHERE `slug` = 'demo-3d-articulated-dragon');

INSERT INTO `products`
  (`category_id`, `product_name`, `slug`, `description`, `brand`, `base_unit`,
   `is_customizable`, `is_active`, `view_count`, `moderation_status`, `rejection_reason`,
   `reviewed_by_user_id`, `reviewed_at`, `search_keywords`, `created_at`, `updated_at`)
SELECT c.`category_id`, 'Chậu cây tự tưới', 'demo-3d-self-watering-planter',
       'Chậu mẫu gồm thân và khay nước; thông tin chỉ phục vụ kiểm thử.', 'Demo Print3D', 'bộ',
       1, 1, 11, 'APPROVED', NULL,
       (SELECT `user_id` FROM `users` WHERE `email` = 'admin@printkraft.vn' AND `role` = 'ADMIN' AND `status` = 'ACTIVE' LIMIT 1),
       '2026-10-02 10:00:00', 'chậu cây in 3D trang trí', '2026-10-01 10:10:00', '2026-10-02 10:00:00'
FROM `categories` c WHERE c.`slug` = 'sample-3d-home'
  AND NOT EXISTS (SELECT 1 FROM `products` WHERE `slug` = 'demo-3d-self-watering-planter');

INSERT INTO `products`
  (`category_id`, `product_name`, `slug`, `description`, `brand`, `base_unit`,
   `is_customizable`, `is_active`, `view_count`, `moderation_status`, `rejection_reason`,
   `reviewed_by_user_id`, `reviewed_at`, `search_keywords`, `created_at`, `updated_at`)
SELECT c.`category_id`, 'Bánh răng prototype', 'demo-3d-engineering-gear',
       'Sản phẩm mẫu đang chờ Admin kiểm duyệt.', 'Demo Print3D', 'cái',
       1, 1, 0, 'PENDING_REVIEW', NULL, NULL, NULL,
       'prototype bánh răng kỹ thuật FDM', '2026-10-03 10:00:00', '2026-10-03 10:00:00'
FROM `categories` c WHERE c.`slug` = 'sample-3d-prototype'
  AND NOT EXISTS (SELECT 1 FROM `products` WHERE `slug` = 'demo-3d-engineering-gear');

INSERT INTO `products`
  (`category_id`, `product_name`, `slug`, `description`, `brand`, `base_unit`,
   `is_customizable`, `is_active`, `view_count`, `moderation_status`, `rejection_reason`,
   `reviewed_by_user_id`, `reviewed_at`, `search_keywords`, `created_at`, `updated_at`)
SELECT c.`category_id`, 'Tượng resin mini', 'demo-3d-resin-miniature',
       'Mô hình mẫu đúc resin, cần xưởng xác nhận màu và kích thước.', 'Demo Print3D', 'cái',
       1, 1, 7, 'APPROVED', NULL,
       (SELECT `user_id` FROM `users` WHERE `email` = 'admin@printkraft.vn' AND `role` = 'ADMIN' AND `status` = 'ACTIVE' LIMIT 1),
       '2026-10-02 10:00:00', 'tượng resin SLA miniature', '2026-10-01 10:15:00', '2026-10-02 10:00:00'
FROM `categories` c WHERE c.`slug` = 'sample-3d-resin'
  AND NOT EXISTS (SELECT 1 FROM `products` WHERE `slug` = 'demo-3d-resin-miniature');

INSERT INTO `products`
  (`category_id`, `product_name`, `slug`, `description`, `brand`, `base_unit`,
   `is_customizable`, `is_active`, `view_count`, `moderation_status`, `rejection_reason`,
   `reviewed_by_user_id`, `reviewed_at`, `search_keywords`, `created_at`, `updated_at`)
SELECT c.`category_id`, 'Giá treo cuộn filament', 'demo-3d-filament-spool-holder',
       'Giá đỡ cuộn filament mẫu, in bằng PLA hoặc PETG.', 'Demo Print3D', 'cái',
       1, 1, 5, 'APPROVED', NULL,
       (SELECT `user_id` FROM `users` WHERE `email` = 'admin@printkraft.vn' AND `role` = 'ADMIN' AND `status` = 'ACTIVE' LIMIT 1),
       '2026-10-02 10:00:00', 'giá treo cuộn filament PLA PETG', '2026-10-01 10:20:00', '2026-10-02 10:00:00'
FROM `categories` c WHERE c.`slug` = 'sample-3d-fdm'
  AND NOT EXISTS (SELECT 1 FROM `products` WHERE `slug` = 'demo-3d-filament-spool-holder');

INSERT INTO `products`
  (`category_id`, `product_name`, `slug`, `description`, `brand`, `base_unit`,
   `is_customizable`, `is_active`, `view_count`, `moderation_status`, `rejection_reason`,
   `reviewed_by_user_id`, `reviewed_at`, `search_keywords`, `created_at`, `updated_at`)
SELECT c.`category_id`, 'Keycap mẫu', 'demo-3d-rejected-keycap',
       'Sản phẩm minh họa trạng thái từ chối kiểm duyệt.', 'Demo Print3D', 'cái',
       1, 1, 0, 'REJECTED', 'Mẫu demo: cần bổ sung thông tin vật liệu và kích thước.',
       (SELECT `user_id` FROM `users` WHERE `email` = 'admin@printkraft.vn' AND `role` = 'ADMIN' AND `status` = 'ACTIVE' LIMIT 1),
       '2026-10-02 11:00:00', 'keycap resin mẫu demo', '2026-10-01 10:25:00', '2026-10-02 11:00:00'
FROM `categories` c WHERE c.`slug` = 'sample-3d-personal'
  AND NOT EXISTS (SELECT 1 FROM `products` WHERE `slug` = 'demo-3d-rejected-keycap');

INSERT INTO `products`
  (`category_id`, `product_name`, `slug`, `description`, `brand`, `base_unit`,
   `is_customizable`, `is_active`, `view_count`, `moderation_status`, `rejection_reason`,
   `reviewed_by_user_id`, `reviewed_at`, `search_keywords`, `created_at`, `updated_at`)
SELECT c.`category_id`, 'Vỏ cảm biến đã ẩn', 'demo-3d-hidden-sensor-case',
       'Sản phẩm demo đã duyệt nhưng chủ động ẩn khỏi catalog.', 'Demo Print3D', 'cái',
       0, 0, 3, 'APPROVED', NULL,
       (SELECT `user_id` FROM `users` WHERE `email` = 'admin@printkraft.vn' AND `role` = 'ADMIN' AND `status` = 'ACTIVE' LIMIT 1),
       '2026-10-02 10:00:00', 'vỏ cảm biến sản phẩm ẩn', '2026-10-01 10:30:00', '2026-10-02 10:00:00'
FROM `categories` c WHERE c.`slug` = 'sample-3d-prototype'
  AND NOT EXISTS (SELECT 1 FROM `products` WHERE `slug` = 'demo-3d-hidden-sensor-case');

INSERT INTO `products`
  (`category_id`, `product_name`, `slug`, `description`, `brand`, `base_unit`,
   `is_customizable`, `is_active`, `view_count`, `moderation_status`, `rejection_reason`,
   `reviewed_by_user_id`, `reviewed_at`, `search_keywords`, `created_at`, `updated_at`)
SELECT c.`category_id`, 'Thẻ tên cá nhân hóa', 'demo-3d-personalized-nameplate',
       'Sản phẩm demo đang chờ duyệt; tên và giá cuối cần seller xác nhận.', 'Demo Print3D', 'cái',
       1, 1, 0, 'PENDING_REVIEW', NULL, NULL, NULL,
       'thẻ tên cá nhân hóa FDM', '2026-10-03 10:05:00', '2026-10-03 10:05:00'
FROM `categories` c WHERE c.`slug` = 'sample-3d-personal'
  AND NOT EXISTS (SELECT 1 FROM `products` WHERE `slug` = 'demo-3d-personalized-nameplate');

INSERT INTO `products`
  (`category_id`, `product_name`, `slug`, `description`, `brand`, `base_unit`,
   `is_customizable`, `is_active`, `view_count`, `moderation_status`, `rejection_reason`,
   `reviewed_by_user_id`, `reviewed_at`, `search_keywords`, `created_at`, `updated_at`)
SELECT c.`category_id`, 'Kệ để bàn nhiều ngăn', 'demo-3d-desk-organizer',
       'Kệ để bàn modular mẫu với các ngăn lắp ghép.', 'Demo Print3D', 'cái',
       1, 1, 9, 'APPROVED', NULL,
       (SELECT `user_id` FROM `users` WHERE `email` = 'admin@printkraft.vn' AND `role` = 'ADMIN' AND `status` = 'ACTIVE' LIMIT 1),
       '2026-10-02 10:00:00', 'kệ bàn modular organizer', '2026-10-01 10:35:00', '2026-10-02 10:00:00'
FROM `categories` c WHERE c.`slug` = 'sample-3d-home'
  AND NOT EXISTS (SELECT 1 FROM `products` WHERE `slug` = 'demo-3d-desk-organizer');

-- 4. Hồ sơ năm xưởng mẫu: 4 đã xác minh, xưởng thứ năm đang chờ duyệt.
INSERT INTO `print_shops`
  (`owner_user_id`, `shop_name`, `slug`, `description`, `logo_url`, `contact_phone`, `contact_email`,
   `address_line`, `province`, `district`, `search_keywords`, `capabilities`, `accepted_file_formats`,
   `max_build_dimensions`, `operating_hours`, `estimated_lead_days`, `service_notes`,
   `average_rating`, `verification_status`, `status`, `created_at`)
SELECT u.`user_id`, 'Hanoi Layer Lab', 'sample-3d-hanoi-layer-lab',
       'Xưởng demo FDM cho phụ kiện và prototype nhỏ.', NULL, '0903100001', u.`email`,
       '12 phố Demo', 'Hà Nội', 'Cầu Giấy', 'in 3D FDM PLA PETG prototype',
       'FDM, PLA, PETG', 'STL, OBJ, 3MF', '220x220x250 mm', '09:00-18:00', 4,
       'Dữ liệu demo; xưởng cần xác nhận giá và thời gian thực tế.', 5.00, 'VERIFIED', 'ACTIVE', '2026-10-01 11:00:00'
FROM `users` u
WHERE u.`email` = 'workshop01@example.test' AND u.`role` = 'CUSTOMER'
  AND u.`password_hash` = 'pbkdf2$210000$OQuEtigHCqxAIhHEvSyeDQ==$qkjoit2Kw/s//PFsODlvwdSmV5XRzL1HyMc9ku9gpyY='
  AND NOT EXISTS (SELECT 1 FROM `print_shops` s WHERE s.`slug` = 'sample-3d-hanoi-layer-lab');

INSERT INTO `print_shops`
  (`owner_user_id`, `shop_name`, `slug`, `description`, `logo_url`, `contact_phone`, `contact_email`,
   `address_line`, `province`, `district`, `search_keywords`, `capabilities`, `accepted_file_formats`,
   `max_build_dimensions`, `operating_hours`, `estimated_lead_days`, `service_notes`,
   `average_rating`, `verification_status`, `status`, `created_at`)
SELECT u.`user_id`, 'Saigon Resin Works', 'sample-3d-saigon-resin-works',
       'Xưởng demo SLA và resin cho miniature, tượng nhỏ và chi tiết bề mặt mịn.', NULL,
       '0903100002', u.`email`, '28 đường Demo', 'TP. Hồ Chí Minh', 'Quận 7',
       'in 3D SLA resin miniature mô hình', 'SLA, resin', 'STL, OBJ', '130x80x160 mm',
       '09:00-18:00', 5, 'Dữ liệu demo; cần seller báo giá cuối.', 0.00,
       'VERIFIED', 'ACTIVE', '2026-10-01 11:05:00'
FROM `users` u
WHERE u.`email` = 'workshop02@example.test' AND u.`role` = 'CUSTOMER'
  AND u.`password_hash` = 'pbkdf2$210000$Re8Kv6tNitxfioaGf4PswQ==$KQq4AdrcBCY7ieAWFCDl2+SJBpvZVIf9mguUCl1y+yM='
  AND NOT EXISTS (SELECT 1 FROM `print_shops` s WHERE s.`slug` = 'sample-3d-saigon-resin-works');

INSERT INTO `print_shops`
  (`owner_user_id`, `shop_name`, `slug`, `description`, `logo_url`, `contact_phone`, `contact_email`,
   `address_line`, `province`, `district`, `search_keywords`, `capabilities`, `accepted_file_formats`,
   `max_build_dimensions`, `operating_hours`, `estimated_lead_days`, `service_notes`,
   `average_rating`, `verification_status`, `status`, `created_at`)
SELECT u.`user_id`, 'Da Nang Fab3D', 'sample-3d-da-nang-fab3d',
       'Xưởng demo FDM đa vật liệu, nhận prototype và chi tiết kích thước vừa.', NULL,
       '0903100003', u.`email`, '36 đường Demo', 'Đà Nẵng', 'Hải Châu',
       'in 3D FDM prototype PETG TPU', 'FDM, PLA, PETG, TPU', 'STL, OBJ, 3MF', '300x300x300 mm',
       '08:30-17:30', 6, 'Dữ liệu demo; seller xác nhận khả năng trước khi nhận việc.', 0.00,
       'VERIFIED', 'ACTIVE', '2026-10-01 11:10:00'
FROM `users` u
WHERE u.`email` = 'workshop03@example.test' AND u.`role` = 'CUSTOMER'
  AND u.`password_hash` = 'pbkdf2$210000$VFxae8GSZohboK8B0bbKgw==$jpuVNvPZytMZOm8WzL1ZQy0vfOEXqWgdBFIWKy3PFaE='
  AND NOT EXISTS (SELECT 1 FROM `print_shops` s WHERE s.`slug` = 'sample-3d-da-nang-fab3d');

INSERT INTO `print_shops`
  (`owner_user_id`, `shop_name`, `slug`, `description`, `logo_url`, `contact_phone`, `contact_email`,
   `address_line`, `province`, `district`, `search_keywords`, `capabilities`, `accepted_file_formats`,
   `max_build_dimensions`, `operating_hours`, `estimated_lead_days`, `service_notes`,
   `average_rating`, `verification_status`, `status`, `created_at`)
SELECT u.`user_id`, 'Can Tho Maker Forge', 'sample-3d-can-tho-maker-forge',
       'Xưởng demo nhận mô hình giáo dục, đồ gá và sản phẩm thử nghiệm FDM.', NULL,
       '0903100004', u.`email`, '48 đường Demo', 'Cần Thơ', 'Ninh Kiều',
       'in 3D FDM đồ gá mô hình prototype', 'FDM, PLA, PETG', 'STL, OBJ', '250x250x250 mm',
       '09:00-17:00', 7, 'Dữ liệu demo; không phải cam kết vận hành.', 0.00,
       'VERIFIED', 'ACTIVE', '2026-10-01 11:15:00'
FROM `users` u
WHERE u.`email` = 'workshop04@example.test' AND u.`role` = 'CUSTOMER'
  AND u.`password_hash` = 'pbkdf2$210000$x5fW2DzhGgFnCKGtdCXHXQ==$KmaXa6h9U8nLUSFKS0LWWqfSHVp9t2r1OlZYgi9GwgQ='
  AND NOT EXISTS (SELECT 1 FROM `print_shops` s WHERE s.`slug` = 'sample-3d-can-tho-maker-forge');

INSERT INTO `print_shops`
  (`owner_user_id`, `shop_name`, `slug`, `description`, `logo_url`, `contact_phone`, `contact_email`,
   `address_line`, `province`, `district`, `search_keywords`, `capabilities`, `accepted_file_formats`,
   `max_build_dimensions`, `operating_hours`, `estimated_lead_days`, `service_notes`,
   `average_rating`, `verification_status`, `status`, `created_at`)
SELECT u.`user_id`, 'Hue Miniature Lab', 'sample-3d-hue-miniature-lab',
       'Xưởng demo làm miniature và mô hình trang trí bằng resin hoặc FDM.', NULL,
       '0903100005', u.`email`, '55 đường Demo', 'Thừa Thiên Huế', 'TP. Huế',
       'in 3D resin FDM miniature mô hình', 'SLA resin, FDM PLA', 'STL, OBJ', '150x100x180 mm',
       '09:00-17:00', 6, 'Shop mẫu đang chờ Admin xác minh.', 0.00,
       'PENDING', 'ACTIVE', '2026-10-01 11:20:00'
FROM `users` u
WHERE u.`email` = 'workshop05@example.test' AND u.`role` = 'CUSTOMER'
  AND u.`password_hash` = 'pbkdf2$210000$dRjCjcL0247Z/n89VZS6QA==$eDwEsVnUZRSDcWHIIyN0uDHOZve8IVKOqj4SuI0hGYs='
  AND NOT EXISTS (SELECT 1 FROM `print_shops` s WHERE s.`slug` = 'sample-3d-hue-miniature-lab');

-- 5. Chào bán riêng của từng xưởng; giá là số demo, không phải báo giá thật.
INSERT INTO `shop_products`
  (`shop_id`, `product_id`, `sku`, `printing_method`, `material`, `unit_price`,
   `minimum_quantity`, `production_days`, `stock_status`, `is_active`, `updated_at`)
SELECT s.`shop_id`, p.`product_id`, 'DEMO-HN-01', 'FDM', 'PLA', 125000.00, 1, 3, 'IN_STOCK', 1, '2026-10-02 12:00:00'
FROM `print_shops` s JOIN `products` p ON p.`slug` = 'demo-3d-phone-stand'
WHERE s.`slug` = 'sample-3d-hanoi-layer-lab'
  AND NOT EXISTS (SELECT 1 FROM `shop_products` WHERE `sku` = 'DEMO-HN-01');

INSERT INTO `shop_products`
  (`shop_id`, `product_id`, `sku`, `printing_method`, `material`, `unit_price`,
   `minimum_quantity`, `production_days`, `stock_status`, `is_active`, `updated_at`)
SELECT s.`shop_id`, p.`product_id`, 'DEMO-HN-02', 'FDM', 'PETG', 99000.00, 1, 4, 'IN_STOCK', 1, '2026-10-02 12:00:00'
FROM `print_shops` s JOIN `products` p ON p.`slug` = 'demo-3d-filament-spool-holder'
WHERE s.`slug` = 'sample-3d-hanoi-layer-lab'
  AND NOT EXISTS (SELECT 1 FROM `shop_products` WHERE `sku` = 'DEMO-HN-02');

INSERT INTO `shop_products`
  (`shop_id`, `product_id`, `sku`, `printing_method`, `material`, `unit_price`,
   `minimum_quantity`, `production_days`, `stock_status`, `is_active`, `updated_at`)
SELECT s.`shop_id`, p.`product_id`, 'DEMO-SG-01', 'SLA', 'Resin tiêu chuẩn', 180000.00, 1, 5, 'PREORDER', 1, '2026-10-02 12:00:00'
FROM `print_shops` s JOIN `products` p ON p.`slug` = 'demo-3d-articulated-dragon'
WHERE s.`slug` = 'sample-3d-saigon-resin-works'
  AND NOT EXISTS (SELECT 1 FROM `shop_products` WHERE `sku` = 'DEMO-SG-01');

INSERT INTO `shop_products`
  (`shop_id`, `product_id`, `sku`, `printing_method`, `material`, `unit_price`,
   `minimum_quantity`, `production_days`, `stock_status`, `is_active`, `updated_at`)
SELECT s.`shop_id`, p.`product_id`, 'DEMO-SG-02', 'SLA', 'Resin xám', 95000.00, 1, 4, 'IN_STOCK', 1, '2026-10-02 12:00:00'
FROM `print_shops` s JOIN `products` p ON p.`slug` = 'demo-3d-resin-miniature'
WHERE s.`slug` = 'sample-3d-saigon-resin-works'
  AND NOT EXISTS (SELECT 1 FROM `shop_products` WHERE `sku` = 'DEMO-SG-02');

INSERT INTO `shop_products`
  (`shop_id`, `product_id`, `sku`, `printing_method`, `material`, `unit_price`,
   `minimum_quantity`, `production_days`, `stock_status`, `is_active`, `updated_at`)
SELECT s.`shop_id`, p.`product_id`, 'DEMO-DN-01', 'FDM', 'PLA', 145000.00, 1, 5, 'IN_STOCK', 1, '2026-10-02 12:00:00'
FROM `print_shops` s JOIN `products` p ON p.`slug` = 'demo-3d-self-watering-planter'
WHERE s.`slug` = 'sample-3d-da-nang-fab3d'
  AND NOT EXISTS (SELECT 1 FROM `shop_products` WHERE `sku` = 'DEMO-DN-01');

INSERT INTO `shop_products`
  (`shop_id`, `product_id`, `sku`, `printing_method`, `material`, `unit_price`,
   `minimum_quantity`, `production_days`, `stock_status`, `is_active`, `updated_at`)
SELECT s.`shop_id`, p.`product_id`, 'DEMO-DN-02', 'FDM', 'PLA', 45000.00, 1, 3, 'PREORDER', 1, '2026-10-02 12:00:00'
FROM `print_shops` s JOIN `products` p ON p.`slug` = 'demo-3d-personalized-nameplate'
WHERE s.`slug` = 'sample-3d-da-nang-fab3d'
  AND NOT EXISTS (SELECT 1 FROM `shop_products` WHERE `sku` = 'DEMO-DN-02');

INSERT INTO `shop_products`
  (`shop_id`, `product_id`, `sku`, `printing_method`, `material`, `unit_price`,
   `minimum_quantity`, `production_days`, `stock_status`, `is_active`, `updated_at`)
SELECT s.`shop_id`, p.`product_id`, 'DEMO-CT-01', 'FDM', 'PETG', 65000.00, 1, 4, 'IN_STOCK', 1, '2026-10-02 12:00:00'
FROM `print_shops` s JOIN `products` p ON p.`slug` = 'demo-3d-engineering-gear'
WHERE s.`slug` = 'sample-3d-can-tho-maker-forge'
  AND NOT EXISTS (SELECT 1 FROM `shop_products` WHERE `sku` = 'DEMO-CT-01');

INSERT INTO `shop_products`
  (`shop_id`, `product_id`, `sku`, `printing_method`, `material`, `unit_price`,
   `minimum_quantity`, `production_days`, `stock_status`, `is_active`, `updated_at`)
SELECT s.`shop_id`, p.`product_id`, 'DEMO-HUE-01', 'SLA', 'Resin tiêu chuẩn', 130000.00, 1, 5, 'PREORDER', 0, '2026-10-02 12:00:00'
FROM `print_shops` s JOIN `products` p ON p.`slug` = 'demo-3d-rejected-keycap'
WHERE s.`slug` = 'sample-3d-hue-miniature-lab'
  AND NOT EXISTS (SELECT 1 FROM `shop_products` WHERE `sku` = 'DEMO-HUE-01');

-- 6. Ảnh sản phẩm: placeholder có URL NULL, không trỏ tới tệp không tồn tại.
INSERT INTO `product_images` (`product_id`, `image_url`, `alt_text`, `is_primary`, `display_order`)
SELECT p.`product_id`, NULL, 'Ảnh demo chưa được tải lên', 0, 1
FROM `products` p WHERE p.`slug` = 'demo-3d-phone-stand'
  AND NOT EXISTS (SELECT 1 FROM `product_images` i WHERE i.`product_id` = p.`product_id` AND i.`alt_text` = 'Ảnh demo chưa được tải lên');

INSERT INTO `product_images` (`product_id`, `image_url`, `alt_text`, `is_primary`, `display_order`)
SELECT p.`product_id`, NULL, 'Ảnh demo chưa được tải lên', 0, 1
FROM `products` p WHERE p.`slug` = 'demo-3d-articulated-dragon'
  AND NOT EXISTS (SELECT 1 FROM `product_images` i WHERE i.`product_id` = p.`product_id` AND i.`alt_text` = 'Ảnh demo chưa được tải lên');

INSERT INTO `product_images` (`product_id`, `image_url`, `alt_text`, `is_primary`, `display_order`)
SELECT p.`product_id`, NULL, 'Ảnh demo chưa được tải lên', 0, 1
FROM `products` p WHERE p.`slug` = 'demo-3d-self-watering-planter'
  AND NOT EXISTS (SELECT 1 FROM `product_images` i WHERE i.`product_id` = p.`product_id` AND i.`alt_text` = 'Ảnh demo chưa được tải lên');

-- 7. Bộ lọc và giá trị lọc cho sản phẩm mẫu.
INSERT INTO `product_filters` (`filter_code`, `filter_name`, `filter_type`, `display_order`, `is_active`)
SELECT 'DEMO_MATERIAL', 'Vật liệu demo', 'SELECT', 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `product_filters` WHERE `filter_code` = 'DEMO_MATERIAL');
INSERT INTO `product_filters` (`filter_code`, `filter_name`, `filter_type`, `display_order`, `is_active`)
SELECT 'DEMO_TECHNOLOGY', 'Công nghệ in demo', 'SELECT', 2, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `product_filters` WHERE `filter_code` = 'DEMO_TECHNOLOGY');
INSERT INTO `product_filters` (`filter_code`, `filter_name`, `filter_type`, `display_order`, `is_active`)
SELECT 'DEMO_COLOR', 'Màu demo', 'SELECT', 3, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `product_filters` WHERE `filter_code` = 'DEMO_COLOR');

INSERT INTO `product_filter_options` (`filter_id`, `option_label`, `option_value`, `display_order`, `is_active`)
SELECT f.`filter_id`, 'PLA', 'PLA', 1, 1 FROM `product_filters` f WHERE f.`filter_code` = 'DEMO_MATERIAL'
  AND NOT EXISTS (SELECT 1 FROM `product_filter_options` o WHERE o.`filter_id` = f.`filter_id` AND o.`option_value` = 'PLA');
INSERT INTO `product_filter_options` (`filter_id`, `option_label`, `option_value`, `display_order`, `is_active`)
SELECT f.`filter_id`, 'PETG', 'PETG', 2, 1 FROM `product_filters` f WHERE f.`filter_code` = 'DEMO_MATERIAL'
  AND NOT EXISTS (SELECT 1 FROM `product_filter_options` o WHERE o.`filter_id` = f.`filter_id` AND o.`option_value` = 'PETG');
INSERT INTO `product_filter_options` (`filter_id`, `option_label`, `option_value`, `display_order`, `is_active`)
SELECT f.`filter_id`, 'Resin', 'RESIN', 3, 1 FROM `product_filters` f WHERE f.`filter_code` = 'DEMO_MATERIAL'
  AND NOT EXISTS (SELECT 1 FROM `product_filter_options` o WHERE o.`filter_id` = f.`filter_id` AND o.`option_value` = 'RESIN');
INSERT INTO `product_filter_options` (`filter_id`, `option_label`, `option_value`, `display_order`, `is_active`)
SELECT f.`filter_id`, 'FDM', 'FDM', 1, 1 FROM `product_filters` f WHERE f.`filter_code` = 'DEMO_TECHNOLOGY'
  AND NOT EXISTS (SELECT 1 FROM `product_filter_options` o WHERE o.`filter_id` = f.`filter_id` AND o.`option_value` = 'FDM');
INSERT INTO `product_filter_options` (`filter_id`, `option_label`, `option_value`, `display_order`, `is_active`)
SELECT f.`filter_id`, 'SLA', 'SLA', 2, 1 FROM `product_filters` f WHERE f.`filter_code` = 'DEMO_TECHNOLOGY'
  AND NOT EXISTS (SELECT 1 FROM `product_filter_options` o WHERE o.`filter_id` = f.`filter_id` AND o.`option_value` = 'SLA');
INSERT INTO `product_filter_options` (`filter_id`, `option_label`, `option_value`, `display_order`, `is_active`)
SELECT f.`filter_id`, 'Đen', 'BLACK', 1, 1 FROM `product_filters` f WHERE f.`filter_code` = 'DEMO_COLOR'
  AND NOT EXISTS (SELECT 1 FROM `product_filter_options` o WHERE o.`filter_id` = f.`filter_id` AND o.`option_value` = 'BLACK');

INSERT INTO `product_filter_values` (`product_id`, `option_id`, `value_text`, `numeric_value`)
SELECT p.`product_id`, o.`option_id`, 'PLA', NULL
FROM `products` p JOIN `product_filters` f ON f.`filter_code` = 'DEMO_MATERIAL'
JOIN `product_filter_options` o ON o.`filter_id` = f.`filter_id` AND o.`option_value` = 'PLA'
WHERE p.`slug` = 'demo-3d-phone-stand'
  AND NOT EXISTS (SELECT 1 FROM `product_filter_values` v WHERE v.`product_id` = p.`product_id` AND v.`option_id` = o.`option_id`);
INSERT INTO `product_filter_values` (`product_id`, `option_id`, `value_text`, `numeric_value`)
SELECT p.`product_id`, o.`option_id`, 'FDM', NULL
FROM `products` p JOIN `product_filters` f ON f.`filter_code` = 'DEMO_TECHNOLOGY'
JOIN `product_filter_options` o ON o.`filter_id` = f.`filter_id` AND o.`option_value` = 'FDM'
WHERE p.`slug` = 'demo-3d-phone-stand'
  AND NOT EXISTS (SELECT 1 FROM `product_filter_values` v WHERE v.`product_id` = p.`product_id` AND v.`option_id` = o.`option_id`);
INSERT INTO `product_filter_values` (`product_id`, `option_id`, `value_text`, `numeric_value`)
SELECT p.`product_id`, o.`option_id`, 'RESIN', NULL
FROM `products` p JOIN `product_filters` f ON f.`filter_code` = 'DEMO_MATERIAL'
JOIN `product_filter_options` o ON o.`filter_id` = f.`filter_id` AND o.`option_value` = 'RESIN'
WHERE p.`slug` = 'demo-3d-resin-miniature'
  AND NOT EXISTS (SELECT 1 FROM `product_filter_values` v WHERE v.`product_id` = p.`product_id` AND v.`option_id` = o.`option_id`);

-- 8. Địa chỉ giao hàng, giỏ hàng và mục yêu thích.
INSERT INTO `addresses` (`user_id`, `recipient_name`, `recipient_phone`, `province`, `district`, `address_line`, `is_default`)
SELECT u.`user_id`, 'Nguyễn Minh Tuấn', '0912345678', 'TP. Hồ Chí Minh', 'Quận 3', '10 đường Demo', 1
FROM `users` u WHERE u.`email` = 'minhtuan@gmail.com'
  AND NOT EXISTS (SELECT 1 FROM `addresses` a WHERE a.`user_id` = u.`user_id` AND a.`address_line` = '10 đường Demo');

INSERT INTO `carts` (`user_id`, `status`, `currency`, `subtotal`, `expires_at`, `created_at`, `updated_at`)
SELECT u.`user_id`, 'ACTIVE', 'VND', 125000.00, NULL, '2026-10-10 08:00:00', '2026-10-10 08:00:00'
FROM `users` u WHERE u.`email` = 'minhtuan@gmail.com'
  AND NOT EXISTS (SELECT 1 FROM `carts` c WHERE c.`user_id` = u.`user_id`);

INSERT INTO `favorites` (`user_id`, `product_id`, `created_at`)
SELECT u.`user_id`, p.`product_id`, '2026-10-05 12:00:00'
FROM `users` u JOIN `products` p ON p.`slug` = 'demo-3d-articulated-dragon'
WHERE u.`email` = 'minhtuan@gmail.com'
  AND NOT EXISTS (SELECT 1 FROM `favorites` f WHERE f.`user_id` = u.`user_id` AND f.`product_id` = p.`product_id`);

-- 9. Tệp thiết kế demo (sample:// không phải tệp tải xuống thật).
INSERT INTO `design_files`
  (`user_id`, `product_id`, `original_filename`, `storage_url`, `preview_url`, `status`, `created_at`)
SELECT u.`user_id`, p.`product_id`, 'demo-custom-nameplate.stl',
       'sample://demo-only/not-a-real-file/demo-custom-nameplate.stl', NULL, 'DEMO_ONLY', '2026-10-08 14:00:00'
FROM `users` u JOIN `products` p ON p.`slug` = 'demo-3d-personalized-nameplate'
WHERE u.`email` = 'minhtuan@gmail.com'
  AND NOT EXISTS (SELECT 1 FROM `design_files` d WHERE d.`user_id` = u.`user_id` AND d.`original_filename` = 'demo-custom-nameplate.stl');

-- Giỏ có một offer mẫu; giữ giá snapshot khớp với offer ban đầu.
INSERT INTO `cart_items`
  (`cart_id`, `shop_product_id`, `design_file_id`, `quantity`, `selected_options`,
   `unit_price_snapshot`, `line_total`, `created_at`)
SELECT c.`cart_id`, sp.`shop_product_id`, NULL, 1,
       '{"color":"đen nhám","material":"PLA","printingMethod":"FDM"}',
       sp.`unit_price`, sp.`unit_price`, '2026-10-10 08:05:00'
FROM `carts` c JOIN `users` u ON u.`user_id` = c.`user_id`
JOIN `shop_products` sp ON sp.`sku` = 'DEMO-HN-01'
WHERE u.`email` = 'minhtuan@gmail.com'
  AND NOT EXISTS (SELECT 1 FROM `cart_items` ci WHERE ci.`cart_id` = c.`cart_id` AND ci.`shop_product_id` = sp.`shop_product_id`);

-- 10. Hai đơn mẫu: COD đã hoàn thành (payment vẫn PENDING) và VNPay thất bại.
INSERT INTO `orders`
  (`order_code`, `user_id`, `address_id`, `subtotal`, `shipping_fee`, `total_amount`, `currency`,
   `status`, `payment_status`, `shipping_status`, `placed_at`, `completed_at`, `cancelled_at`)
SELECT 'DEMO-ORDER-0001', u.`user_id`, a.`address_id`, 125000.00, 20000.00, 145000.00, 'VND',
       'Đã hoàn thành', 'COD (thanh toán khi nhận hàng)', 'Đã giao',
       '2026-10-10 09:00:00', '2026-10-13 16:00:00', NULL
FROM `users` u JOIN `addresses` a ON a.`user_id` = u.`user_id` AND a.`address_line` = '10 đường Demo'
WHERE u.`email` = 'minhtuan@gmail.com'
  AND NOT EXISTS (SELECT 1 FROM `orders` o WHERE o.`order_code` = 'DEMO-ORDER-0001');

INSERT INTO `orders`
  (`order_code`, `user_id`, `address_id`, `subtotal`, `shipping_fee`, `total_amount`, `currency`,
   `status`, `payment_status`, `shipping_status`, `placed_at`, `completed_at`, `cancelled_at`)
SELECT 'DEMO-ORDER-0002', u.`user_id`, a.`address_id`, 180000.00, 25000.00, 205000.00, 'VND',
       'Chờ thanh toán', 'Chưa thanh toán', 'Chưa có',
       '2026-10-20 10:00:00', NULL, NULL
FROM `users` u JOIN `addresses` a ON a.`user_id` = u.`user_id` AND a.`address_line` = '10 đường Demo'
WHERE u.`email` = 'minhtuan@gmail.com'
  AND NOT EXISTS (SELECT 1 FROM `orders` o WHERE o.`order_code` = 'DEMO-ORDER-0002');

INSERT INTO `order_items`
  (`order_id`, `product_id`, `shop_product_id`, `shop_id`, `design_file_id`,
   `product_name_snapshot`, `shop_name_snapshot`, `quantity`, `unit_price`, `line_total`, `status`)
SELECT o.`order_id`, p.`product_id`, sp.`shop_product_id`, s.`shop_id`, NULL,
       p.`product_name`, s.`shop_name`, 1, 125000.00, 125000.00, 'Đã hoàn thành'
FROM `orders` o JOIN `products` p ON p.`slug` = 'demo-3d-phone-stand'
JOIN `shop_products` sp ON sp.`sku` = 'DEMO-HN-01'
JOIN `print_shops` s ON s.`slug` = 'sample-3d-hanoi-layer-lab'
WHERE o.`order_code` = 'DEMO-ORDER-0001'
  AND NOT EXISTS (SELECT 1 FROM `order_items` oi WHERE oi.`order_id` = o.`order_id` AND oi.`shop_product_id` = sp.`shop_product_id`);

INSERT INTO `order_items`
  (`order_id`, `product_id`, `shop_product_id`, `shop_id`, `design_file_id`,
   `product_name_snapshot`, `shop_name_snapshot`, `quantity`, `unit_price`, `line_total`, `status`)
SELECT o.`order_id`, p.`product_id`, sp.`shop_product_id`, s.`shop_id`, NULL,
       p.`product_name`, s.`shop_name`, 1, 180000.00, 180000.00, 'Chờ thanh toán'
FROM `orders` o JOIN `products` p ON p.`slug` = 'demo-3d-articulated-dragon'
JOIN `shop_products` sp ON sp.`sku` = 'DEMO-SG-01'
JOIN `print_shops` s ON s.`slug` = 'sample-3d-saigon-resin-works'
WHERE o.`order_code` = 'DEMO-ORDER-0002'
  AND NOT EXISTS (SELECT 1 FROM `order_items` oi WHERE oi.`order_id` = o.`order_id` AND oi.`shop_product_id` = sp.`shop_product_id`);

-- COD chỉ là trạng thái phương thức thanh toán; không ghi giao dịch thành công.
INSERT INTO `payments`
  (`order_id`, `payment_method`, `provider`, `provider_transaction_ref`, `checkout_url`,
   `amount`, `status`, `failure_reason`, `paid_at`, `created_at`, `updated_at`)
SELECT o.`order_id`, 'COD', 'COD', 'DEMO-SEED-COD-0001', NULL, 145000.00,
       'PENDING', NULL, NULL, '2026-10-10 09:01:00', '2026-10-10 09:01:00'
FROM `orders` o WHERE o.`order_code` = 'DEMO-ORDER-0001'
  AND NOT EXISTS (SELECT 1 FROM `payments` WHERE `provider_transaction_ref` = 'DEMO-SEED-COD-0001');

INSERT INTO `payments`
  (`order_id`, `payment_method`, `provider`, `provider_transaction_ref`, `checkout_url`,
   `amount`, `status`, `failure_reason`, `paid_at`, `created_at`, `updated_at`)
SELECT o.`order_id`, 'VNPay', 'VNPay', 'DEMO-SEED-FAILED-0002', NULL, 205000.00,
       'FAILED', 'Dữ liệu demo: không có giao dịch thật hoặc cổng thanh toán.', NULL,
       '2026-10-20 10:01:00', '2026-10-20 10:01:00'
FROM `orders` o WHERE o.`order_code` = 'DEMO-ORDER-0002'
  AND NOT EXISTS (SELECT 1 FROM `payments` WHERE `provider_transaction_ref` = 'DEMO-SEED-FAILED-0002');

-- 11. Lịch sử trạng thái đơn; ghi chú có dấu DEMO để dễ phân biệt dữ liệu mẫu.
INSERT INTO `order_status_history` (`order_id`, `changed_by_user_id`, `old_status`, `new_status`, `note`, `changed_at`)
SELECT o.`order_id`, u.`user_id`, NULL, 'Chờ thanh toán', 'DEMO: tạo đơn hàng', '2026-10-10 09:00:00'
FROM `orders` o JOIN `users` u ON u.`email` = 'minhtuan@gmail.com'
WHERE o.`order_code` = 'DEMO-ORDER-0001'
  AND NOT EXISTS (SELECT 1 FROM `order_status_history` h WHERE h.`order_id` = o.`order_id` AND h.`note` = 'DEMO: tạo đơn hàng');

INSERT INTO `order_status_history` (`order_id`, `changed_by_user_id`, `old_status`, `new_status`, `note`, `changed_at`)
SELECT o.`order_id`, u.`user_id`, 'Chờ thanh toán', 'Đã xác nhận', 'DEMO: xác nhận COD', '2026-10-10 10:00:00'
FROM `orders` o JOIN `users` u ON u.`email` = 'minhtuan@gmail.com'
WHERE o.`order_code` = 'DEMO-ORDER-0001'
  AND NOT EXISTS (SELECT 1 FROM `order_status_history` h WHERE h.`order_id` = o.`order_id` AND h.`note` = 'DEMO: xác nhận COD');

INSERT INTO `order_status_history` (`order_id`, `changed_by_user_id`, `old_status`, `new_status`, `note`, `changed_at`)
SELECT o.`order_id`, u.`user_id`, 'Đã xác nhận', 'Đang xử lý', 'DEMO: bắt đầu sản xuất', '2026-10-10 10:30:00'
FROM `orders` o JOIN `users` u ON u.`email` = 'minhtuan@gmail.com'
WHERE o.`order_code` = 'DEMO-ORDER-0001'
  AND NOT EXISTS (SELECT 1 FROM `order_status_history` h WHERE h.`order_id` = o.`order_id` AND h.`note` = 'DEMO: bắt đầu sản xuất');

INSERT INTO `order_status_history` (`order_id`, `changed_by_user_id`, `old_status`, `new_status`, `note`, `changed_at`)
SELECT o.`order_id`, u.`user_id`, 'Đang xử lý', 'Đang giao hàng', 'DEMO: bàn giao vận chuyển', '2026-10-13 13:00:00'
FROM `orders` o JOIN `users` u ON u.`email` = 'minhtuan@gmail.com'
WHERE o.`order_code` = 'DEMO-ORDER-0001'
  AND NOT EXISTS (SELECT 1 FROM `order_status_history` h WHERE h.`order_id` = o.`order_id` AND h.`note` = 'DEMO: bàn giao vận chuyển');

INSERT INTO `order_status_history` (`order_id`, `changed_by_user_id`, `old_status`, `new_status`, `note`, `changed_at`)
SELECT o.`order_id`, u.`user_id`, 'Đang giao hàng', 'Đã hoàn thành', 'DEMO: hoàn tất giao hàng', '2026-10-13 16:00:00'
FROM `orders` o JOIN `users` u ON u.`email` = 'minhtuan@gmail.com'
WHERE o.`order_code` = 'DEMO-ORDER-0001'
  AND NOT EXISTS (SELECT 1 FROM `order_status_history` h WHERE h.`order_id` = o.`order_id` AND h.`note` = 'DEMO: hoàn tất giao hàng');

INSERT INTO `order_status_history` (`order_id`, `changed_by_user_id`, `old_status`, `new_status`, `note`, `changed_at`)
SELECT o.`order_id`, u.`user_id`, NULL, 'Chờ thanh toán', 'DEMO: tạo đơn chờ thanh toán', '2026-10-20 10:00:00'
FROM `orders` o JOIN `users` u ON u.`email` = 'minhtuan@gmail.com'
WHERE o.`order_code` = 'DEMO-ORDER-0002'
  AND NOT EXISTS (SELECT 1 FROM `order_status_history` h WHERE h.`order_id` = o.`order_id` AND h.`note` = 'DEMO: tạo đơn chờ thanh toán');

-- 12. Tiến độ sản xuất mẫu theo đúng chuỗi stage của Workshop Portal.
INSERT INTO `production_updates` (`order_item_id`, `shop_id`, `created_by_user_id`, `stage`, `progress_percent`, `message`, `created_at`)
SELECT oi.`order_item_id`, s.`shop_id`, u.`user_id`, 'ACCEPTED', 0, 'DEMO: xưởng tiếp nhận mẫu', '2026-10-10 10:31:00'
FROM `order_items` oi JOIN `orders` o ON o.`order_id` = oi.`order_id`
JOIN `print_shops` s ON s.`slug` = 'sample-3d-hanoi-layer-lab'
JOIN `users` u ON u.`email` = 'workshop01@example.test'
WHERE o.`order_code` = 'DEMO-ORDER-0001'
  AND NOT EXISTS (SELECT 1 FROM `production_updates` pu WHERE pu.`order_item_id` = oi.`order_item_id` AND pu.`stage` = 'ACCEPTED');

INSERT INTO `production_updates` (`order_item_id`, `shop_id`, `created_by_user_id`, `stage`, `progress_percent`, `message`, `created_at`)
SELECT oi.`order_item_id`, s.`shop_id`, u.`user_id`, 'PRINTING', 55, 'DEMO: đang in mẫu', '2026-10-10 11:00:00'
FROM `order_items` oi JOIN `orders` o ON o.`order_id` = oi.`order_id`
JOIN `print_shops` s ON s.`slug` = 'sample-3d-hanoi-layer-lab'
JOIN `users` u ON u.`email` = 'workshop01@example.test'
WHERE o.`order_code` = 'DEMO-ORDER-0001'
  AND NOT EXISTS (SELECT 1 FROM `production_updates` pu WHERE pu.`order_item_id` = oi.`order_item_id` AND pu.`stage` = 'PRINTING');

INSERT INTO `production_updates` (`order_item_id`, `shop_id`, `created_by_user_id`, `stage`, `progress_percent`, `message`, `created_at`)
SELECT oi.`order_item_id`, s.`shop_id`, u.`user_id`, 'POST_PROCESSING', 85, 'DEMO: đang hoàn thiện bề mặt', '2026-10-10 12:00:00'
FROM `order_items` oi JOIN `orders` o ON o.`order_id` = oi.`order_id`
JOIN `print_shops` s ON s.`slug` = 'sample-3d-hanoi-layer-lab'
JOIN `users` u ON u.`email` = 'workshop01@example.test'
WHERE o.`order_code` = 'DEMO-ORDER-0001'
  AND NOT EXISTS (SELECT 1 FROM `production_updates` pu WHERE pu.`order_item_id` = oi.`order_item_id` AND pu.`stage` = 'POST_PROCESSING');

INSERT INTO `production_updates` (`order_item_id`, `shop_id`, `created_by_user_id`, `stage`, `progress_percent`, `message`, `created_at`)
SELECT oi.`order_item_id`, s.`shop_id`, u.`user_id`, 'READY_FOR_SHIPPING', 100, 'DEMO: sản phẩm mẫu sẵn sàng giao', '2026-10-13 12:30:00'
FROM `order_items` oi JOIN `orders` o ON o.`order_id` = oi.`order_id`
JOIN `print_shops` s ON s.`slug` = 'sample-3d-hanoi-layer-lab'
JOIN `users` u ON u.`email` = 'workshop01@example.test'
WHERE o.`order_code` = 'DEMO-ORDER-0001'
  AND NOT EXISTS (SELECT 1 FROM `production_updates` pu WHERE pu.`order_item_id` = oi.`order_item_id` AND pu.`stage` = 'READY_FOR_SHIPPING');

-- 13. Vận chuyển mẫu; mã theo dõi mang tiền tố DEMO.
INSERT INTO `shipments` (`order_id`, `shop_id`, `carrier`, `tracking_number`, `status`, `shipped_at`, `delivered_at`)
SELECT o.`order_id`, s.`shop_id`, 'Demo carrier', 'DEMO-TRACK-0001', 'Đã giao',
       '2026-10-13 13:00:00', '2026-10-13 16:00:00'
FROM `orders` o JOIN `print_shops` s ON s.`slug` = 'sample-3d-hanoi-layer-lab'
WHERE o.`order_code` = 'DEMO-ORDER-0001'
  AND NOT EXISTS (SELECT 1 FROM `shipments` WHERE `tracking_number` = 'DEMO-TRACK-0001');

-- 14. Một yêu cầu báo giá chưa được seller trả lời; không tự tạo mức giá.
INSERT INTO `quote_requests`
  (`user_id`, `product_id`, `preferred_shop_id`, `company_name`, `requested_quantity`,
   `required_by`, `quoted_amount`, `seller_response_note`, `status`)
SELECT u.`user_id`, p.`product_id`, s.`shop_id`, 'Công ty Demo 3D', 50,
       '2026-12-31', NULL, NULL, 'Chờ xử lý'
FROM `users` u JOIN `products` p ON p.`slug` = 'demo-3d-self-watering-planter'
JOIN `print_shops` s ON s.`slug` = 'sample-3d-da-nang-fab3d'
WHERE u.`email` = 'minhtuan@gmail.com'
  AND NOT EXISTS (SELECT 1 FROM `quote_requests` q WHERE q.`user_id` = u.`user_id` AND q.`company_name` = 'Công ty Demo 3D');

-- 15. Đánh giá mẫu gắn với dòng hàng đã hoàn thành; nội dung được đánh dấu demo.
INSERT INTO `reviews` (`order_item_id`, `user_id`, `product_id`, `shop_id`, `rating`, `review_text`, `is_published`)
SELECT oi.`order_item_id`, u.`user_id`, oi.`product_id`, oi.`shop_id`, 5,
       'Đánh giá minh họa trong dữ liệu demo, không phải phản hồi khách hàng thật.', 1
FROM `order_items` oi JOIN `orders` o ON o.`order_id` = oi.`order_id`
JOIN `users` u ON u.`email` = 'minhtuan@gmail.com'
WHERE o.`order_code` = 'DEMO-ORDER-0001'
  AND NOT EXISTS (SELECT 1 FROM `reviews` r WHERE r.`order_item_id` = oi.`order_item_id`);

COMMIT;

-- Kiểm tra nhanh sau COMMIT: đếm marker mẫu ở tất cả 22 bảng.
SELECT 'users' AS sample_table, COUNT(*) AS sample_rows FROM `users`
 WHERE `email` IN ('admin@printkraft.vn', 'minhtuan@gmail.com', 'workshop01@example.test',
                   'workshop02@example.test', 'workshop03@example.test', 'workshop04@example.test', 'workshop05@example.test')
UNION ALL SELECT 'addresses', COUNT(*) FROM `addresses` a JOIN `users` u ON u.`user_id` = a.`user_id`
 WHERE u.`email` = 'minhtuan@gmail.com' AND a.`address_line` = '10 đường Demo'
UNION ALL SELECT 'carts', COUNT(*) FROM `carts` c JOIN `users` u ON u.`user_id` = c.`user_id`
 WHERE u.`email` = 'minhtuan@gmail.com' AND c.`status` = 'ACTIVE'
UNION ALL SELECT 'cart_items', COUNT(*) FROM `cart_items` ci JOIN `carts` c ON c.`cart_id` = ci.`cart_id`
 JOIN `users` u ON u.`user_id` = c.`user_id` JOIN `shop_products` sp ON sp.`shop_product_id` = ci.`shop_product_id`
 WHERE u.`email` = 'minhtuan@gmail.com' AND sp.`sku` = 'DEMO-HN-01'
UNION ALL SELECT 'favorites', COUNT(*) FROM `favorites` f JOIN `users` u ON u.`user_id` = f.`user_id`
 JOIN `products` p ON p.`product_id` = f.`product_id`
 WHERE u.`email` = 'minhtuan@gmail.com' AND p.`slug` = 'demo-3d-articulated-dragon'
UNION ALL SELECT 'categories', COUNT(*) FROM `categories` WHERE `slug` LIKE 'sample-3d-%'
UNION ALL SELECT 'products', COUNT(*) FROM `products` WHERE `slug` LIKE 'demo-3d-%'
UNION ALL SELECT 'product_images', COUNT(*) FROM `product_images` WHERE `alt_text` = 'Ảnh demo chưa được tải lên'
UNION ALL SELECT 'product_filters', COUNT(*) FROM `product_filters`
 WHERE `filter_code` IN ('DEMO_MATERIAL', 'DEMO_TECHNOLOGY', 'DEMO_COLOR')
UNION ALL SELECT 'product_filter_options', COUNT(*) FROM `product_filter_options` o
 JOIN `product_filters` f ON f.`filter_id` = o.`filter_id`
 WHERE f.`filter_code` IN ('DEMO_MATERIAL', 'DEMO_TECHNOLOGY', 'DEMO_COLOR')
UNION ALL SELECT 'product_filter_values', COUNT(*) FROM `product_filter_values` v
 JOIN `products` p ON p.`product_id` = v.`product_id` WHERE p.`slug` LIKE 'demo-3d-%'
UNION ALL SELECT 'print_shops', COUNT(*) FROM `print_shops` WHERE `slug` LIKE 'sample-3d-%'
UNION ALL SELECT 'shop_products', COUNT(*) FROM `shop_products` WHERE `sku` LIKE 'DEMO-%'
UNION ALL SELECT 'design_files', COUNT(*) FROM `design_files` WHERE `original_filename` = 'demo-custom-nameplate.stl'
UNION ALL SELECT 'orders', COUNT(*) FROM `orders` WHERE `order_code` LIKE 'DEMO-%'
UNION ALL SELECT 'order_items', COUNT(*) FROM `order_items` oi JOIN `orders` o ON o.`order_id` = oi.`order_id`
 WHERE o.`order_code` LIKE 'DEMO-%'
UNION ALL SELECT 'payments', COUNT(*) FROM `payments` WHERE `provider_transaction_ref` LIKE 'DEMO-SEED-%'
UNION ALL SELECT 'quote_requests', COUNT(*) FROM `quote_requests` WHERE `company_name` = 'Công ty Demo 3D'
UNION ALL SELECT 'order_status_history', COUNT(*) FROM `order_status_history` WHERE `note` LIKE 'DEMO:%'
UNION ALL SELECT 'production_updates', COUNT(*) FROM `production_updates` WHERE `message` LIKE 'DEMO:%'
UNION ALL SELECT 'shipments', COUNT(*) FROM `shipments` WHERE `tracking_number` LIKE 'DEMO-%'
UNION ALL SELECT 'reviews', COUNT(*) FROM `reviews` WHERE `review_text` LIKE 'Đánh giá minh họa trong dữ liệu demo%';
