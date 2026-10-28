-- =============================================================================
-- PrintKraft — DỮ LIỆU MẪU CHO BẢNG product_comments (Hỏi đáp & Bình luận sản phẩm)
-- =============================================================================
-- File này chứa dữ liệu mẫu câu hỏi của khách hàng và câu trả lời của người bán / shop.
-- Sử dụng:
--   mysql --default-character-set=utf8mb4 -u root -p printkraft < sample_product_comments.sql
-- =============================================================================

USE `printkraft`;

-- Xóa dữ liệu cũ của bảng product_comments để tránh trùng lặp
DELETE FROM `product_comments`;
ALTER TABLE `product_comments` AUTO_INCREMENT = 1;

INSERT INTO `product_comments` (`comment_id`, `product_id`, `user_id`, `parent_id`, `content`, `is_seller_reply`, `created_at`) VALUES
-- Sản phẩm 1: Cốc sứ trắng 2 mặt
(1, 1, 2, NULL, 'Shop cho mình hỏi hình in trên cốc khi rửa bằng nước nóng hoặc cho vào lò vi sóng có bị phai màu hay bong tróc không ạ?', 0, DATE_SUB(NOW(), INTERVAL 4 DAY) + INTERVAL 2 HOUR),
(2, 1, 8, 1, 'Chào bạn Tuấn! Cốc sứ được in bằng công nghệ chuyển nhiệt Sublimation ở nhiệt độ cao (trên 200°C), mực in thấm sâu vào lớp men sứ nên dùng nước nóng và lò vi sóng thoải mái, không sợ phai màu hay bong tróc nhé ạ!', 1, DATE_SUB(NOW(), INTERVAL 4 DAY) + INTERVAL 2 HOUR + INTERVAL 35 MINUTE),
(3, 1, 2, 1, 'Dạ cảm ơn shop nhiều nhé, mình vừa đặt 2 chiếc để tặng sinh nhật bạn rồi ạ.', 0, DATE_SUB(NOW(), INTERVAL 4 DAY) + INTERVAL 3 HOUR),
(4, 1, 4, NULL, 'Xưởng có nhận in theo yêu cầu từ 1 cái không shop, hay phải đặt theo lô số lượng lớn?', 0, DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 4 HOUR),
(5, 1, 8, 4, 'Chào bạn Mai, bên mình nhận in từ 1 chiếc theo hình ảnh cá nhân bạn gửi nhé! Nếu đặt số lượng từ 10 chiếc trở lên sẽ có giá sỉ ưu đãi hơn ạ.', 1, DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 4 HOUR + INTERVAL 20 MINUTE),
(6, 1, 5, NULL, 'Thời gian in và giao hàng về Cần Thơ thì khoảng mấy ngày nhận được vậy shop?', 0, DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 1 HOUR),
(7, 1, 8, 6, 'Chào bạn Huy! Thời gian in khoảng 1 ngày, sau đó đơn vị vận chuyển giao về Cần Thơ mất từ 2-3 ngày bạn nha.', 1, DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 1 HOUR + INTERVAL 40 MINUTE),

-- Sản phẩm 2: Áo thun Oversize Cotton
(8, 2, 3, NULL, 'Mình cao 1m73, nặng 68kg thì nên chọn size L hay XL để mặc kiểu form rộng rãi thoải mái vậy shop?', 0, DATE_SUB(NOW(), INTERVAL 5 DAY) + INTERVAL 3 HOUR),
(9, 2, 9, 8, 'Chào bạn Bình! Với chiều cao và cân nặng của bạn, nếu thích form rộng vừa phải tôn dáng thì size L là chuẩn đẹp, còn nếu muốn mặc phong cách streetwear oversize thùng thình thì chọn size XL bạn nhé!', 1, DATE_SUB(NOW(), INTERVAL 5 DAY) + INTERVAL 4 HOUR),
(10, 2, 3, 8, 'Dạ cảm ơn shop tư vấn nhiệt tình, mình vừa đặt size L màu đen rồi nha.', 0, DATE_SUB(NOW(), INTERVAL 5 DAY) + INTERVAL 4 HOUR + INTERVAL 15 MINUTE),
(11, 2, 6, NULL, 'Hình in DTG này giặt máy có bị nứt hình không shop? Có lưu ý gì khi giặt lần đầu không ạ?', 0, DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 1 HOUR),
(12, 2, 9, 11, 'Chào bạn Khoa! Công nghệ in DTG mực ngấm trực tiếp vào sợi vải cotton nên rất mềm và không bị gãy nứt như in decal ép nhiệt. Lần giặt đầu tiên bạn nên lộn trái áo, giặt bằng nước lạnh và không dùng chất tẩy mạnh là áo sẽ rất bền nhé.', 1, DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 1 HOUR + INTERVAL 50 MINUTE),
(13, 2, 11, NULL, 'Xưởng có hỗ trợ kiểm tra file thiết kế logo trước khi in lên áo không?', 0, DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 5 HOUR),
(14, 2, 9, 13, 'Chào bạn Bảo! Có bạn nhé, sau khi bạn đặt hàng hoặc gửi yêu cầu, kỹ thuật viên bên mình sẽ kiểm tra độ phân giải file in và gửi ảnh mockup để bạn duyệt trước khi tiến hành in thực tế ạ.', 1, DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 5 HOUR + INTERVAL 25 MINUTE),

-- Sản phẩm 3: Túi vải canvas dày
(15, 3, 7, NULL, 'Bên mình là câu lạc bộ sinh viên muốn in khoảng 50 chiếc túi vải canvas có logo CLB, thời gian sản xuất và giao về Thủ Đức mất bao lâu shop?', 0, DATE_SUB(NOW(), INTERVAL 4 DAY) + INTERVAL 6 HOUR),
(16, 3, 10, 15, 'Chào bạn Hạnh! Đơn hàng 50 túi xưởng in mất khoảng 3-4 ngày, sau đó giao đến Thủ Đức trong ngày là bạn nhận được nhé. Bạn có thể gửi file logo qua mục Yêu cầu báo giá để bên mình hỗ trợ lên mockup trước ạ.', 1, DATE_SUB(NOW(), INTERVAL 4 DAY) + INTERVAL 7 HOUR),
(17, 3, 12, NULL, 'Túi có khóa kéo miệng túi và ngăn phụ nhỏ bên trong không shop ơi?', 0, DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 8 HOUR),
(18, 3, 10, 17, 'Chào bạn My! Mẫu túi canvas dày tiêu chuẩn có tích hợp khóa kéo zip kim loại chắc chắn và có 1 ngăn phụ nhỏ bên trong để đựng chìa khóa, điện thoại rất tiện lợi nhé bạn.', 1, DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 8 HOUR + INTERVAL 30 MINUTE),

-- Sản phẩm 4: Sticker decal cuộn
(19, 4, 5, NULL, 'Sticker này dán nón bảo hiểm hoặc bình nước đi mưa có bị thấm nước rách không shop?', 0, DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 2 HOUR),
(20, 4, 8, 19, 'Chào bạn Huy! Sticker được in trên chất liệu Decal nhựa PVC cán màng bóng chống nước 100%, chống tia UV nên dán nón bảo hiểm, xe máy, bình nước rửa thoải mái không sợ rách hay phai màu bạn nha.', 1, DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 2 HOUR + INTERVAL 45 MINUTE),

-- Sản phẩm 5: Hoodie Basic Nỉ Bông
(21, 5, 11, NULL, 'Chất nỉ bông của áo có bị xù lông hay dính lông vào áo trong khi mặc không shop?', 0, DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 9 HOUR),
(22, 5, 9, 21, 'Chào bạn Bảo! Áo dùng vải nỉ bông cotton chải kỹ cao cấp (định lượng 380gsm), mặt trong đã được xử lý chống rụng lông nên không bị dính vào áo thun mặc trong đâu bạn nhé.', 1, DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 10 HOUR),

-- Sản phẩm 6: Bình giữ nhiệt in logo
(23, 6, 4, NULL, 'Xưởng có hỗ trợ khắc tên cá nhân lên bình kèm logo không ạ, và có tốn thêm phí không shop?', 0, DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 10 HOUR),
(24, 6, 8, 23, 'Chào bạn Mai! Xưởng miễn phí khắc laser tên riêng và logo cho khách hàng đặt trên PrintKraft nhé! Khi đặt hàng bạn chỉ cần ghi chú nội dung muốn khắc là được ạ.', 1, DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 11 HOUR),

-- Sản phẩm 8: Namecard giấy C300
(25, 8, 6, NULL, 'Một hộp namecard gồm bao nhiêu cái vậy shop? Giấy có được cán màng mờ 2 mặt không?', 0, DATE_SUB(NOW(), INTERVAL 4 DAY) + INTERVAL 11 HOUR),
(26, 8, 10, 25, 'Chào bạn Khoa! Quy cách chuẩn là 1 hộp gồm 100 chiếc namecard đựng trong hộp mica trong suốt. Giấy Couche 300gsm được cán màng mờ chống trầy xước cả 2 mặt rất sang trọng nhé bạn.', 1, DATE_SUB(NOW(), INTERVAL 4 DAY) + INTERVAL 12 HOUR),

-- Sản phẩm 11: Áo Polo Thể Thao
(27, 11, 3, NULL, 'Áo này mặc chơi thể thao cầu lông hay chạy bộ có thoáng mát và thấm hút mồ hôi tốt không shop?', 0, DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 3 HOUR),
(28, 11, 9, 27, 'Chào bạn Bình! Áo may từ chất liệu thun cá sấu mắt chim co giãn 4 chiều, có tính năng thoát khí và thấm hút mồ hôi cực nhanh, rất phù hợp cho các hoạt động thể thao vận động mạnh nhé!', 1, DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 3 HOUR + INTERVAL 35 MINUTE),

-- Sản phẩm 12: Sổ tay bìa da PU
(29, 12, 7, NULL, 'Sổ này ruột giấy bao nhiêu trang và giấy có dùng được bút mực máy mà không bị nhòe sang trang sau không shop?', 0, DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 6 HOUR),
(30, 12, 8, 29, 'Chào bạn Hạnh! Sổ gồm 160 trang giấy kẻ ngang ngà chống lóa, định lượng giấy 100gsm dày dặn nên viết bút bi, bút gel hay bút máy đều không lo bị lem sang mặt sau nhé!', 1, DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 6 HOUR + INTERVAL 40 MINUTE),

-- Sản phẩm 14: Bút bi kim loại khắc tên
(31, 14, 12, NULL, 'Bút dùng loại ngòi nào và sau này hết mực thì có dễ tìm ngòi thay thế không shop?', 0, DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 8 HOUR),
(32, 14, 10, 31, 'Chào bạn My! Bút sử dụng ngòi bi tiêu chuẩn Parker 0.7mm mực xanh, ngòi này rất phổ biến và bạn có thể dễ dàng mua ngòi thay thế tại các nhà sách nhé.', 1, DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 8 HOUR + INTERVAL 30 MINUTE);
