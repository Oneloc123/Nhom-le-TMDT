-- =============================================================================
-- PrintKraft — SAMPLE DATA (MẪU, TÙY CHỌN — CHỈ CHO MÔI TRƯỜNG PHÁT TRIỂN)
-- =============================================================================
-- File này KHÔNG phải schema. Nó chỉ nạp dữ liệu mẫu (demo) để trang sản phẩm,
-- danh mục và gian hàng có dữ liệu hiển thị sau khi import printkraft_database_safe.sql.
-- KHÔNG import file này vào môi trường thật nếu bạn không muốn dữ liệu mẫu.
--
-- Cách dùng (sau khi đã import schema an toàn):
--   mysql -u root -p printkraft < printkraft_sample_data.sql
--
-- Tài khoản mẫu (đổi mật khẩu ngay khi dùng thật):
--   admin@printkraft.vn / admin123        (ADMIN)
--   minhtuan@gmail.com  / 123456          (CUSTOMER)
--   Mật khẩu lưu dạng băm PBKDF2 (không plaintext).
-- =============================================================================

USE printkraft;

-- ---------------------------------------------------------------------------
-- Users (2)
-- ---------------------------------------------------------------------------
INSERT INTO `users` (`user_id`, `full_name`, `email`, `phone`, `password_hash`, `role`, `status`, `created_at`) VALUES
(1, 'Quản trị viên', 'admin@printkraft.vn', '0901000001', 'pbkdf2$210000$zrfD+LrRM1ld0BMdRCtLXQ==$4VtK1qg++9Poqf5RVyQvj70i3WiT6Fl6m7M/5FnEO6o=', 'ADMIN', 'ACTIVE', NOW()),
(2, 'Nguyễn Minh Tuấn', 'minhtuan@gmail.com', '0912345678', 'pbkdf2$210000$YJN3Plki0bYpOHxSP2kiqA==$T6Dzel5zx/TumJomdpj4VF3dEHMvWLEKmlabGrZElQk=', 'CUSTOMER', 'ACTIVE', NOW());

-- ---------------------------------------------------------------------------
-- Categories (7) — khớp các liên kết danh mục trên trang chủ
-- ---------------------------------------------------------------------------
INSERT INTO `categories` (`category_id`, `parent_id`, `category_name`, `slug`, `is_active`, `display_order`) VALUES
(1, NULL, 'Áo thun & Hoodie',        'ao-thun-hoodie',        1, 1),
(2, NULL, 'Cốc & Bình giữ nhiệt',    'coc-binh-giu-nhiet',    1, 2),
(3, NULL, 'Túi vải & Phụ kiện',      'tui-vai-phu-kien',      1, 3),
(4, NULL, 'Bao bì & Nhãn dán',       'bao-bi-nhan-dan',       1, 4),
(5, NULL, 'Poster & Canvas',          'poster-canvas',         1, 5),
(6, NULL, 'Namecard & Văn phòng phẩm','namecard-van-phong-pham',1, 6),
(7, NULL, 'Quà tặng doanh nghiệp',   'qua-tang-doanh-nghiep',  1, 7);

-- ---------------------------------------------------------------------------
-- Print shops (5)
-- ---------------------------------------------------------------------------
INSERT INTO `print_shops` (`shop_id`, `owner_user_id`, `shop_name`, `slug`, `description`, `logo_url`,
  `contact_phone`, `province`, `district`, `search_keywords`, `average_rating`, `verification_status`, `status`, `created_at`) VALUES
(1, NULL, 'Mực Tím Studio', 'muc-tim-studio',
  'Chuyên cốc sứ, bình giữ nhiệt và quà tặng cá nhân hóa.', NULL,
  '0901234567', 'TP. Hồ Chí Minh', 'Quận 1', 'Cốc, bình giữ nhiệt, quà tặng', 4.90, 'VERIFIED', 'ACTIVE', NOW()),
(2, NULL, 'Xưởng Mây In', 'xuong-may-in',
  'Xưởng may và in áo thun, hoodie theo yêu cầu.', NULL,
  '0912345678', 'Hà Nội', 'Cầu Giấy', 'Áo thun, hoodie, đồng phục', 4.80, 'VERIFIED', 'ACTIVE', NOW()),
(3, NULL, 'Canvas Lab', 'canvas-lab',
  'Sản xuất túi canvas và quà tặng doanh nghiệp số lượng lớn.', NULL,
  '0923456789', 'Đà Nẵng', 'Hải Châu', 'Túi vải, quà tặng doanh nghiệp', 4.70, 'VERIFIED', 'ACTIVE', NOW()),
(4, NULL, 'Nhãn Xinh', 'nhan-xinh',
  'In sticker, tem nhãn và bao bì nhỏ lẻ giao nhanh.', NULL,
  '0934567890', 'TP. Hồ Chí Minh', 'Quận Bình Thạnh', 'Sticker, decal, tem nhãn', 4.90, 'VERIFIED', 'ACTIVE', NOW()),
(5, NULL, 'Poster House', 'poster-house',
  'In poster, canvas, namecard và văn phòng phẩm mỹ thuật.', NULL,
  '0945678901', 'Bình Dương', 'Thị xã Dầu Tiếng', 'Poster, canvas, namecard', 4.80, 'VERIFIED', 'ACTIVE', NOW());

-- ---------------------------------------------------------------------------
-- Products (10)
-- ---------------------------------------------------------------------------
INSERT INTO `products` (`product_id`, `category_id`, `product_name`, `slug`, `description`, `brand`,
  `base_unit`, `is_customizable`, `is_active`, `search_keywords`, `created_at`, `updated_at`) VALUES
(1, 2, 'Cốc sứ trắng 2 mặt', 'coc-su-trang-2-mat',
  'Cốc sứ trắng phủ bóng, in hình sắc nét ở cả hai mặt.', 'PrintKraft', 'cái', 1, 1, 'cốc sứ in ảnh', NOW(), NOW()),
(2, 1, 'Áo thun Oversize Cotton', 'ao-thun-oversize-cotton',
  'Áo cotton 100%, form oversize, phù hợp in DTG từ một chiếc.', 'PrintKraft', 'cái', 1, 1, 'áo thun in DTG', NOW(), NOW()),
(3, 3, 'Túi vải canvas dày', 'tui-vai-canvas-day',
  'Túi canvas định lượng cao, quai chắc và phù hợp in lụa.', 'PrintKraft', 'cái', 1, 1, 'túi canvas in lụa', NOW(), NOW()),
(4, 4, 'Sticker decal cuộn', 'sticker-decal-cuon',
  'Sticker decal chống nước, bế demi theo hình dạng thiết kế.', 'PrintKraft', 'tem', 1, 1, 'sticker decal bế demi', NOW(), NOW()),
(5, 1, 'Hoodie Basic Nỉ Bông', 'hoodie-basic-ni-bong',
  'Hoodie nỉ bông ấm, nhận in DTF số lượng nhỏ và vừa.', 'PrintKraft', 'cái', 1, 1, 'hoodie in DTF', NOW(), NOW()),
(6, 2, 'Bình giữ nhiệt in logo', 'binh-giu-nhiet-in-logo',
  'Bình inox 500ml, khắc laser logo bền đẹp, giữ nhiệt tốt.', 'PrintKraft', 'cái', 1, 1, 'bình giữ nhiệt khắc laser', NOW(), NOW()),
(7, 5, 'Poster A2 giấy mỹ thuật', 'poster-a2-giay-my-thuat',
  'Poster A2 trên giấy mỹ thuật 300gsm, màu in nổi bật.', 'PrintKraft', 'tờ', 1, 1, 'poster A2 giấy mỹ thuật', NOW(), NOW()),
(8, 6, 'Namecard giấy mỹ thuật 300gsm', 'namecard-giay-my-thuat-300gsm',
  'Namecard cán mờ, bo góc tùy chọn, nhận từ 100 tấm.', 'PrintKraft', 'tấm', 1, 1, 'namecard cán mờ', NOW(), NOW()),
(9, 5, 'Canvas treo tường 40x60', 'canvas-treo-tuong-40x60',
  'Tranh canvas căng khung gỗ, phù hợp trang trí văn phòng và nhà ở.', 'PrintKraft', 'bức', 1, 1, 'tranh canvas khung gỗ', NOW(), NOW()),
(10, 7, 'Túi tote doanh nghiệp', 'tui-tote-doanh-nghiep',
  'Túi tote may theo nhận diện thương hiệu, hỗ trợ số lượng lớn.', 'PrintKraft', 'cái', 1, 1, 'túi tote doanh nghiệp', NOW(), NOW());

-- ---------------------------------------------------------------------------
-- Shop offers — shop_products (giá / vật liệu / số lượng tối thiểu / thời gian in)
-- ---------------------------------------------------------------------------
INSERT INTO `shop_products` (`shop_product_id`, `shop_id`, `product_id`, `sku`, `printing_method`,
  `material`, `unit_price`, `minimum_quantity`, `production_days`, `stock_status`, `is_active`, `updated_at`) VALUES
(1, 1, 1, 'MT-COC-01',  'In Sublimation',    'Sứ trắng',        65000.00,  1,   3, 'IN_STOCK', 1, NOW()),
(2, 2, 2, 'MI-AO-01',   'In DTG',            'Cotton 100%',    129000.00,  1,   5, 'IN_STOCK', 1, NOW()),
(3, 3, 3, 'CL-TUI-01',  'In lụa',            'Canvas 300gsm',   42000.00, 50,   7, 'IN_STOCK', 1, NOW()),
(4, 4, 4, 'NX-STK-01',  'In kỹ thuật số',     'Decal PVC',        3500.00,100,   2, 'IN_STOCK', 1, NOW()),
(5, 2, 5, 'MI-HOOD-01', 'In DTF',            'Nỉ bông',        289000.00, 10,   7, 'IN_STOCK', 1, NOW()),
(6, 1, 6, 'MT-BINH-01', 'Khắc laser',        'Inox 500ml',     215000.00, 20,   5, 'IN_STOCK', 1, NOW()),
(7, 5, 7, 'PH-POST-01', 'In UV',             'Giấy mỹ thuật 300gsm', 89000.00, 1, 3, 'IN_STOCK', 1, NOW()),
(8, 5, 8, 'PH-NAME-01', 'In offset',         'Giấy mỹ thuật 300gsm',  1200.00, 100, 4, 'IN_STOCK', 1, NOW()),
(9, 5, 9, 'PH-CANV-01', 'In canvas',         'Canvas + khung gỗ', 245000.00, 1, 5, 'IN_STOCK', 1, NOW()),
(10, 3, 10, 'CL-TOTE-01','In lụa',           'Canvas tote',      55000.00, 50,  10, 'IN_STOCK', 1, NOW()),
(11, 3, 1, 'CL-COC-02',  'In UV',             'Sứ trắng',        72000.00,  5,   4, 'IN_STOCK', 1, NOW()),
(12, 1, 2, 'MT-AO-02',   'In DTF',            'Cotton 65/35',    99000.00,  5,   4, 'IN_STOCK', 1, NOW());
