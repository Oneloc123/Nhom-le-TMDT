# Chính sách tài chính PrintKraft

## Chính sách đã chốt

- Hoa hồng nền tảng: **10% cố định** trên giá trị sản phẩm.
- Không hỗ trợ tỷ lệ riêng theo từng xưởng.
- Chu kỳ quyết toán: **hàng tháng theo tháng dương lịch**.
- COD: **nền tảng là bên nhận và đối soát tiền COD**. Chỉ ghi nhận doanh thu sau khi có xác nhận tiền đã được đơn vị vận chuyển chuyển về nền tảng.

## Công thức

Với từng dòng hàng của đơn:

- Giá trị tính hoa hồng = `order_items.line_total`.
- Hoa hồng = làm tròn 2 chữ số thập phân của giá trị tính hoa hồng × 10%.
- Phải trả xưởng = giá trị tính hoa hồng − hoa hồng.
- Phí vận chuyển không thuộc cơ sở tính hoa hồng.

Ví dụ: một dòng hàng 1.000.000 VND → hoa hồng 100.000 VND, phần xưởng 900.000 VND. Đây là ví dụ minh họa.

## Quy tắc ghi nhận

1. Đơn VNPay: chỉ phân bổ sau IPN hợp lệ, đúng số tiền và payment được đánh dấu PAID.
2. Đơn COD: trạng thái payment PENDING không phải tiền đã thu. Admin chỉ xác nhận thu COD sau khi đối chiếu chứng từ chuyển tiền từ đơn vị vận chuyển; mã tham chiếu phải duy nhất.
3. Tỷ lệ hoa hồng được snapshot vào từng allocation. Thay đổi chính sách tương lai không làm thay đổi đơn cũ.
4. Hủy đơn/thất bại thanh toán không tạo allocation. Hoàn tiền cần bút toán điều chỉnh tương ứng; không sửa/xóa ledger đã ghi.
5. Đơn có nhiều xưởng phải phân bổ theo từng order_item/shop_id.
6. Chỉ các khoản đã thu thực tế mới được đưa vào số dư quyết toán.

## Quyết toán hàng tháng

Kỳ là từ ngày đầu tháng đến ngày cuối tháng (inclusive), theo thời điểm tiền được xác nhận thu. Hệ thống tổng hợp riêng từng xưởng. Tạo kỳ quyết toán chỉ khóa/tập hợp số liệu; trạng thái PAID chỉ được ghi khi Admin xác nhận đã chuyển khoản và nhập mã tham chiếu giao dịch.

## Migration

Chạy `migrate_20261125_platform_finance.sql` sau khi backup, chọn đúng database `printkraft`, kiểm tra MariaDB/MySQL version và xác nhận các bảng `orders`, `order_items`, `payments`, `print_shops`, `users` tồn tại với khóa chính BIGINT tương thích.

Migration chỉ tạo bảng tài chính mới và chèn cấu hình mặc định 10% nếu chưa có. Không sửa/xóa dữ liệu đơn hàng, payment hoặc order item hiện hữu. Đơn cũ không tự động được backfill vì không đủ bằng chứng thu tiền/đối soát.

## Giới hạn tích hợp COD

Repository hiện không có API/credential tích hợp hãng vận chuyển để tự nhận callback tiền COD. Chế độ PLATFORM vì vậy cần bước Admin xác nhận có chứng từ chuyển tiền từ carrier. Không được tự động đánh dấu COD đã thu khi đơn vừa được tạo hoặc giao thành công.

## Kiểm tra sau migration

```sql
SELECT * FROM platform_finance_settings WHERE setting_id = 1;
SHOW TABLES LIKE '%financial%';
SELECT COUNT(*) AS orders_before_backfill FROM orders;
```

Đây là lớp schema và chính sách nền. Chưa được coi là luồng tài chính hoàn chỉnh cho tới khi API/service tích hợp vào callback thanh toán, màn hình xác nhận COD, quyết toán và hoàn tiền được triển khai, kiểm thử trên database staging.
