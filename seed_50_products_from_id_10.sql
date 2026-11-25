-- PrintKraft: thêm 50 sản phẩm catalog 3D, danh mục 011–060.
-- Chạy sau printkraft_database_safe.sql và printkraft_sample_data.sql trên database printkraft.
-- Seed hiện có đã chứa 10 sản phẩm đầu; vì vậy file này thêm 50 sản phẩm tiếp theo (011–060).
-- product_id để AUTO_INCREMENT tự cấp để không ghi đè sản phẩm/đơn hàng đang tồn tại.
-- Với database demo mới, các ID thường là 11–60; đường dẫn ảnh luôn dựa trên ID thực tế.
-- Ảnh demo chỉ có trong gói đính kèm cho sản phẩm 001–003. Với sản phẩm khác, hãy đặt
-- 3 ảnh WebP vào Store/images/product-{product_id}/image-1.webp ... image-3.webp.
-- Có thể thay đổi image_url trong product_images nếu muốn dùng JPG/PNG hoặc đường dẫn khác.
-- Các giá/offer dưới đây là dữ liệu mẫu để kiểm thử, không phải báo giá thật.

SET NAMES utf8mb4;
USE `printkraft`;
START TRANSACTION;

-- 1. Thêm các sản phẩm chưa tồn tại theo slug; chạy lại file không nhân đôi sản phẩm.
INSERT INTO `products`
  (`category_id`, `product_name`, `slug`, `description`, `brand`, `base_unit`,
   `is_customizable`, `is_active`, `view_count`, `moderation_status`,
   `rejection_reason`, `reviewed_by_user_id`, `reviewed_at`,
   `search_keywords`, `created_at`, `updated_at`)
SELECT c.`category_id`,
       d.`product_name`,
       CONCAT('catalog-3d-', d.`product_no`, '-', d.`slug_suffix`),
       CONCAT('Sản phẩm in 3D thuộc danh mục mẫu. ', d.`product_name`,
              '. Vui lòng xác nhận vật liệu, kích thước, màu sắc và thời gian sản xuất với xưởng.'),
       'PrintKraft Catalog',
       'cái',
       1, 1, 0, 'APPROVED', NULL,
       (SELECT u.`user_id` FROM `users` u
         WHERE u.`email` = 'admin@printkraft.vn' AND u.`role` = 'ADMIN'
           AND u.`status` = 'ACTIVE' LIMIT 1),
       CURRENT_TIMESTAMP,
       CONCAT(d.`product_name`, ' in 3D 3D printing'),
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
        SELECT '011' AS product_no, 'Khủng long T-Rex' AS product_name, 'trex-model' AS slug_suffix, 'sample-3d-miniature' AS category_slug
        UNION ALL SELECT '012', 'Khủng long Triceratops', 'triceratops-model', 'sample-3d-miniature'
        UNION ALL SELECT '013', 'Mô hình robot', 'robot-model', 'sample-3d-miniature'
        UNION ALL SELECT '014', 'Mô hình phi hành gia', 'astronaut-figurine', 'sample-3d-miniature'
        UNION ALL SELECT '015', 'Hiệp sĩ fantasy', 'fantasy-knight', 'sample-3d-miniature'
        UNION ALL SELECT '016', 'Pháp sư fantasy', 'fantasy-wizard', 'sample-3d-miniature'
        UNION ALL SELECT '017', 'Rồng fantasy', 'fantasy-dragon', 'sample-3d-miniature'
        UNION ALL SELECT '018', 'Phượng hoàng', 'phoenix-model', 'sample-3d-miniature'
        UNION ALL SELECT '019', 'Kỳ lân', 'unicorn-model', 'sample-3d-miniature'
        UNION ALL SELECT '020', 'Mèo low-poly', 'low-poly-cat', 'sample-3d-miniature'
        UNION ALL SELECT '021', 'Chó low-poly', 'low-poly-dog', 'sample-3d-miniature'
        UNION ALL SELECT '022', 'Mô hình cú', 'owl-figurine', 'sample-3d-miniature'
        UNION ALL SELECT '023', 'Mô hình voi', 'elephant-figurine', 'sample-3d-miniature'
        UNION ALL SELECT '024', 'Mô hình cá voi', 'whale-model', 'sample-3d-miniature'
        UNION ALL SELECT '025', 'Mô hình cá mập', 'shark-model', 'sample-3d-miniature'
        UNION ALL SELECT '026', 'Mô hình bạch tuộc', 'octopus-model', 'sample-3d-miniature'
        UNION ALL SELECT '027', 'Mô hình ếch', 'frog-figurine', 'sample-3d-miniature'
        UNION ALL SELECT '028', 'Mô hình gấu', 'bear-figurine', 'sample-3d-miniature'
        UNION ALL SELECT '029', 'Mô hình thỏ', 'rabbit-figurine', 'sample-3d-miniature'
        UNION ALL SELECT '030', 'Mô hình sư tử', 'lion-figurine', 'sample-3d-miniature'
        UNION ALL SELECT '031', 'Đầu tượng cổ điển', 'classical-bust', 'sample-3d-miniature'
        UNION ALL SELECT '032', 'Tượng chân dung', 'portrait-bust', 'sample-3d-miniature'
        UNION ALL SELECT '033', 'Miniature chiến binh', 'warrior-miniature', 'sample-3d-miniature'
        UNION ALL SELECT '034', 'Miniature cung thủ', 'archer-miniature', 'sample-3d-miniature'
        UNION ALL SELECT '035', 'Miniature pháp sư', 'mage-miniature', 'sample-3d-miniature'
        UNION ALL SELECT '036', 'Miniature zombie', 'zombie-miniature', 'sample-3d-miniature'
        UNION ALL SELECT '037', 'Miniature bộ xương', 'skeleton-miniature', 'sample-3d-miniature'
        UNION ALL SELECT '038', 'Miniature Orc', 'orc-miniature', 'sample-3d-miniature'
        UNION ALL SELECT '039', 'Miniature Elf', 'elf-miniature', 'sample-3d-miniature'
        UNION ALL SELECT '040', 'Miniature chiến binh không gian', 'space-marine-miniature', 'sample-3d-miniature'
        UNION ALL SELECT '041', 'Bình hoa xoắn', 'spiral-vase', 'sample-3d-home'
        UNION ALL SELECT '042', 'Bình hoa low-poly', 'low-poly-vase', 'sample-3d-home'
        UNION ALL SELECT '043', 'Chậu cây treo', 'hanging-planter', 'sample-3d-home'
        UNION ALL SELECT '044', 'Chậu cây hình đầu', 'head-planter', 'sample-3d-home'
        UNION ALL SELECT '045', 'Tượng trừu tượng', 'abstract-sculpture', 'sample-3d-home'
        UNION ALL SELECT '046', 'Tranh tường hình học', 'geometric-wall-art', 'sample-3d-home'
        UNION ALL SELECT '047', 'Chụp đèn', 'lamp-shade', 'sample-3d-home'
        UNION ALL SELECT '048', 'Đèn ngủ lithophane', 'lithophane-night-light', 'sample-3d-home'
        UNION ALL SELECT '049', 'Ảnh lithophane', 'photo-lithophane', 'sample-3d-home'
        UNION ALL SELECT '050', 'Khung đồng hồ', 'clock-frame', 'sample-3d-home'
        UNION ALL SELECT '051', 'Chân nến', 'candle-holder', 'sample-3d-home'
        UNION ALL SELECT '052', 'Đèn lồng tealight', 'tealight-lantern', 'sample-3d-home'
        UNION ALL SELECT '053', 'Khay đốt hương', 'incense-holder', 'sample-3d-home'
        UNION ALL SELECT '054', 'Hộp trang sức', 'jewelry-box', 'sample-3d-home'
        UNION ALL SELECT '055', 'Đĩa đựng nhẫn', 'ring-dish', 'sample-3d-home'
        UNION ALL SELECT '056', 'Khung ảnh', 'photo-frame', 'sample-3d-home'
        UNION ALL SELECT '057', 'Chặn sách', 'bookend', 'sample-3d-home'
        UNION ALL SELECT '058', 'Móc treo tường', 'wall-hook', 'sample-3d-home'
        UNION ALL SELECT '059', 'Biển treo cửa', 'door-sign', 'sample-3d-home'
        UNION ALL SELECT '060', 'Số nhà', 'house-numbers', 'sample-3d-home'
) AS d
JOIN `categories` c ON c.`slug` = d.`category_slug`
WHERE NOT EXISTS (
    SELECT 1 FROM `products` existing
    WHERE existing.`slug` = CONCAT('catalog-3d-', d.`product_no`, '-', d.`slug_suffix')
);

-- 2. Tạo ba ảnh có thể tùy chỉnh cho mỗi sản phẩm mới.
-- Tên file luôn dùng ID sản phẩm trong database, không phụ thuộc thứ tự insert.
INSERT INTO `product_images`
  (`product_id`, `image_url`, `alt_text`, `is_primary`, `display_order`)
SELECT p.`product_id`,
       CONCAT('Store/images/product-', LPAD(p.`product_id`, 3, '0'),
              '/image-', slots.`image_no`, '.webp'),
       CONCAT(p.`product_name`, ' - ảnh ', slots.`image_no`),
       IF(slots.`image_no` = 1, 1, 0),
       slots.`image_no`
FROM `products` p
JOIN (SELECT 1 AS `image_no` UNION ALL SELECT 2 UNION ALL SELECT 3) AS slots
  ON 1 = 1
WHERE p.`slug` LIKE 'catalog-3d-%'
  AND NOT EXISTS (
      SELECT 1 FROM `product_images` i
      WHERE i.`product_id` = p.`product_id`
        AND i.`display_order` = slots.`image_no`
        AND i.`image_url` IS NOT NULL AND TRIM(i.`image_url`) <> ''
  );

-- 3. Map chín ảnh mẫu đã gửi vào ba sản phẩm seed hiện có.
-- Các đường dẫn được tính theo product_id thực tế; với database seed mới chúng là 001, 002, 003.
INSERT INTO `product_images`
  (`product_id`, `image_url`, `alt_text`, `is_primary`, `display_order`)
SELECT p.`product_id`,
       CONCAT('Store/images/product-', LPAD(p.`product_id`, 3, '0'),
              '/image-', slots.`image_no`, '.webp'),
       CONCAT(p.`product_name`, ' - ảnh ', slots.`image_no`),
       IF(slots.`image_no` = 1, 1, 0),
       slots.`image_no`
FROM `products` p
JOIN (
    SELECT 'demo-3d-phone-stand' AS `product_slug`, 1 AS `image_no`
    UNION ALL SELECT 'demo-3d-phone-stand', 2
    UNION ALL SELECT 'demo-3d-phone-stand', 3
    UNION ALL SELECT 'demo-3d-articulated-dragon', 1
    UNION ALL SELECT 'demo-3d-articulated-dragon', 2
    UNION ALL SELECT 'demo-3d-articulated-dragon', 3
    UNION ALL SELECT 'demo-3d-self-watering-planter', 1
    UNION ALL SELECT 'demo-3d-self-watering-planter', 2
    UNION ALL SELECT 'demo-3d-self-watering-planter', 3
) AS slots ON slots.`product_slug` = p.`slug`
WHERE NOT EXISTS (
    SELECT 1 FROM `product_images` i
    WHERE i.`product_id` = p.`product_id`
      AND i.`display_order` = slots.`image_no`
      AND i.`image_url` IS NOT NULL AND TRIM(i.`image_url`) <> ''
);

-- 4. Thêm offer mẫu vào xưởng VERIFIED đầu tiên để sản phẩm xuất hiện trong catalog.
-- Nếu database chưa có xưởng VERIFIED, sản phẩm vẫn được thêm; chạy lại file sau khi có xưởng.
INSERT INTO `shop_products`
  (`shop_id`, `product_id`, `sku`, `printing_method`, `material`, `unit_price`,
   `minimum_quantity`, `production_days`, `stock_status`, `is_active`, `updated_at`)
SELECT s.`shop_id`,
       p.`product_id`,
       CONCAT('PK-CAT-', p.`product_id`, '-01'),
       'FDM', 'PLA',
       50000 + MOD(p.`product_id`, 8) * 5000,
       1, 4, 'IN_STOCK', 1, CURRENT_TIMESTAMP
FROM `products` p
JOIN (
    SELECT `shop_id` FROM `print_shops`
    WHERE `verification_status` = 'VERIFIED'
      AND (`status` IS NULL OR `status` NOT IN ('INACTIVE', 'BLOCKED'))
    ORDER BY `shop_id`
    LIMIT 1
) AS s ON 1 = 1
WHERE p.`slug` LIKE 'catalog-3d-%'
  AND NOT EXISTS (
      SELECT 1 FROM `shop_products` sp
      WHERE sp.`product_id` = p.`product_id`
        AND sp.`shop_id` = s.`shop_id`
  )
  AND NOT EXISTS (
      SELECT 1 FROM `shop_products` duplicate_sku
      WHERE duplicate_sku.`sku` = CONCAT('PK-CAT-', p.`product_id`, '-01')
  );

COMMIT;

-- Kiểm tra nhanh:
SELECT `product_id`, `product_name`, `slug`
FROM `products`
WHERE `slug` LIKE 'catalog-3d-%'
ORDER BY `product_id`;

SELECT p.`product_id`, p.`product_name`, COUNT(i.`image_id`) AS `image_rows`
FROM `products` p
LEFT JOIN `product_images` i ON i.`product_id` = p.`product_id`
 AND i.`image_url` IS NOT NULL AND TRIM(i.`image_url`) <> ''
WHERE p.`slug` LIKE 'catalog-3d-%'
GROUP BY p.`product_id`, p.`product_name`
ORDER BY p.`product_id`;
