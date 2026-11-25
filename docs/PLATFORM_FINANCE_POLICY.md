# Chính sách tài chính PrintKraft

## Chính sách được triển khai

- Hoa hồng nền tảng: **10% cố định** trên giá trị sản phẩm.
- Áp dụng cùng một tỷ lệ cho toàn bộ xưởng; không có cấu hình riêng từng xưởng.
- Cơ sở tính hoa hồng là `order_items.line_total`; **không tính phí vận chuyển**. Phí vận chuyển được ghi riêng vào tài khoản clearing để đối soát, không được cộng vào doanh thu hoa hồng.
- Quyết toán theo tháng dương lịch, theo từng xưởng.
- COD do nền tảng tiếp nhận và đối soát. COD chỉ trở thành tiền đã thu sau khi Admin kiểm tra chứng từ chuyển tiền thực tế và ghi mã tham chiếu của đơn vị vận chuyển.

Ví dụ: dòng hàng 1.000.000 VND → hoa hồng 100.000 VND, khoản phải trả xưởng 900.000 VND. Đây là minh họa công thức.

## Dòng tiền và trạng thái

1. **VNPay:** chỉ tạo phân bổ sau IPN hợp lệ, số tiền khớp, payment thành `PAID` và trạng thái đơn là `Đã thanh toán`.
2. **COD:** khi khách chọn COD, payment vẫn là `PENDING`; đơn xác nhận không đồng nghĩa PrintKraft đã nhận tiền. Admin vào **Tài chính & Hoa hồng → COD chờ đối soát**, kiểm tra tiền carrier thực chuyển và nhập đơn vị vận chuyển + mã tham chiếu. Hệ thống ghi receipt, cập nhật payment và tạo phân bổ trong cùng transaction.
3. Phân bổ lưu theo từng `order_item_id`, gồm giá trị hàng, tỷ lệ hoa hồng snapshot, tiền hoa hồng và tiền xưởng. Đơn nhiều xưởng được tách theo từng dòng hàng.
4. Tỷ lệ được snapshot khi ghi nhận giao dịch. Không thay đổi hồi tố khi chính sách mặc định tương lai được điều chỉnh. Phí vận chuyển được hạch toán thành `SHIPPING_CLEARING`, và hoàn phí vận chuyển thành `REFUND_SHIPPING_FEE`; hai bút toán này không làm thay đổi hoa hồng hoặc tiền hàng của xưởng.
5. Đơn chưa thanh toán, payment thất bại hoặc đơn cũ chưa có bằng chứng thu tiền không tự động được tính doanh thu.

## Quyết toán tháng

- Admin chọn tháng đã kết thúc; API từ chối tháng hiện tại/tương lai.
- Hệ thống tổng hợp toàn bộ phân bổ còn chưa quyết toán đến hết tháng đã chọn, gồm số dư chưa trả từ các kỳ trước và điều chỉnh hoàn tiền còn mở.
- Kỳ được nhóm theo xưởng và tiền tệ. Các khoản phân bổ được gắn vào kỳ để không tính trùng ở tháng sau.
- Chỉ kỳ ở trạng thái `READY` mới có thể xác nhận đã chuyển. Admin phải nhập mã giao dịch ngân hàng; khi đó trạng thái chuyển thành `PAID` và ghi bút toán chi xưởng.
- Nếu hoàn tiền làm số dư xưởng của kỳ đang chờ chi không dương, kỳ chuyển sang `REVIEW_REQUIRED` và không có nút xác nhận thanh toán. Không tự động chuyển khoản hoặc âm tiền cho xưởng.

## Hoàn tiền và điều chỉnh

- Hoàn tiền thành công của yêu cầu đổi trả/hoàn tiền được ghi nhận theo từng dòng hàng; hoàn payment thủ công được phân bổ trên các dòng hàng theo tỷ lệ, tối đa bằng giá trị sản phẩm chưa hoàn. Phần phí vận chuyển không thuộc hoa hồng.
- Hoa hồng được đảo theo tỷ lệ snapshot của dòng hàng. Phần phải thu hồi từ xưởng được ghi thành bút toán điều chỉnh riêng; không sửa/xóa các bút toán cũ.
- Nếu kỳ của xưởng chưa được chi, điều chỉnh làm giảm kỳ đó. Nếu kỳ đã PAID, khoản thu hồi được giữ mở để bù trừ vào kỳ quyết toán tiếp theo.
- Đơn cũ không có allocation snapshot sẽ không tự được ghi nhận điều chỉnh; cần quy trình rà soát riêng.

## Khu vực quản trị

Mở **Admin → Tài chính & Hoa hồng** (`/admin/finance`) để xem:
- Tổng giá trị sản phẩm đã ghi nhận, hoa hồng nền tảng, khoản phải trả xưởng và COD chờ đối soát.
- Danh sách COD cần xác nhận tiền về.
- Quyết toán theo tháng từng xưởng và xác nhận lệnh chuyển tiền.
- Phân bổ theo từng đơn hàng/dòng sản phẩm.
- Bút toán điều chỉnh hoàn tiền, tiền đảo hoa hồng và tiền thu hồi xưởng.

Các thao tác POST sử dụng lớp bảo vệ Admin và CSRF hiện hữu của ứng dụng.

## Cấu trúc dữ liệu được thêm

- `platform_finance_settings`: chính sách cố định 10%, monthly, COD platform.
- `order_financial_allocations`: snapshot phân bổ từng dòng hàng.
- `financial_cod_receipts`: xác nhận tiền COD thực tế về nền tảng.
- `seller_monthly_settlements`: kỳ quyết toán từng xưởng.
- `financial_ledger`: nhật ký ghi nhận thu, phải trả, hoa hồng, điều chỉnh, chuyển tiền.
- `financial_refund_adjustments`: điều chỉnh hoàn tiền và số dư cần thu hồi.

Migration là bổ sung, không xóa hoặc backfill tự động các đơn/payment cũ.

## Thứ tự triển khai database

1. Sao lưu database và xác nhận đang chọn đúng schema `printkraft`. Không chạy file bootstrap có lệnh xóa database trên môi trường đang có dữ liệu.
2. Xác nhận các bảng nền `users`, `orders`, `order_items`, `payments`, `print_shops` có khóa chính BIGINT tương thích.
3. Đảm bảo các migration nghiệp vụ hiện hữu đã được áp dụng nếu sử dụng tính năng liên quan: `migrate_admin_business_queues.sql` (đối soát/hoàn tiền Admin) và `migrate_customer_returns_refunds.sql` (yêu cầu đổi trả/hoàn tiền).
4. Chạy `migrate_20261125_platform_finance.sql` một lần trên database đích. Có thể chạy lại phần tạo bảng; phần procedure bổ sung cột kỳ hoàn tiền là idempotent.
5. Kiểm tra:

```sql
SELECT VERSION() AS server_version, DATABASE() AS selected_database;
SELECT * FROM platform_finance_settings WHERE setting_id=1;
SHOW TABLES LIKE '%financial%';
SHOW CREATE TABLE order_financial_allocations;
SHOW CREATE TABLE seller_monthly_settlements;
SELECT COUNT(*) AS legacy_orders FROM orders;
```

Kết quả policy phải là 10.00 / PRODUCT_SUBTOTAL / MONTHLY / PLATFORM. Không có allocation tự sinh cho đơn cũ.

## COD integration boundary

Repository không có credential hoặc webhook hãng vận chuyển để tự động biết carrier đã chuyển tiền COD về PrintKraft. Vì vậy, nút xác nhận COD là thao tác đối soát thủ công có audit reference, không phải callback tự động. Chỉ sử dụng sau khi kiểm chứng sao kê/chứng từ thực tế.

## Kiểm thử và giới hạn xác nhận

Chạy `./mvnw --batch-mode --no-transfer-progress clean verify` và các bước Admin JavaScript trong GitHub Actions. Cần chạy migration trên database staging MariaDB 10.4 và kiểm thử thực tế VNPay/COD, hoàn tiền và quyết toán trước khi dùng production.

Việc chỉnh sửa mã nguồn không tự thực thi migration trên máy chạy PrintKraft và không tự chuyển tiền tới xưởng.
