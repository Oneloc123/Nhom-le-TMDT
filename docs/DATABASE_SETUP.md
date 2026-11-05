# Cài đặt database PrintKraft

Dùng hai file SQL trong repository theo đúng tình huống. Không có migration runner; ứng dụng không tự thay đổi schema khi khởi động.

## Hai file SQL

- `printkraft_database_safe.sql`: cấu trúc 22 bảng hiện tại, tương thích MariaDB 10.4.32. Không có `DROP DATABASE`, `DROP TABLE` hoặc `TRUNCATE`.
- `printkraft_sample_data.sql`: dữ liệu demo liên kết cho cả 22 bảng. Chỉ chạy ở database phát triển/kiểm thử; không chạy production.
- `printkraft_database.sql` là file cũ có lệnh xóa database. Không chạy file này.

`CREATE TABLE IF NOT EXISTS` không thay thế bảng đã tồn tại. Phần bổ sung cuối file schema thêm các cột/chỉ mục Admin và Workshop còn thiếu trong schema nền đã biết. Nó không sửa định nghĩa cột sai, khóa chính/ngoại không tương thích hoặc mọi kiểu schema khác.

## Trường hợp A — tạo database mới, đang trống

1. Mở Navicat (hoặc công cụ MariaDB tương đương) và xác nhận đúng máy chủ.
2. Mở `printkraft_database_safe.sql` và chạy toàn bộ file. File tạo/chọn database `printkraft`, sau đó tạo bảng và cấu trúc bổ sung.
3. Chỉ ở môi trường development/test, mở `printkraft_sample_data.sql` và chạy toàn bộ file để thêm dữ liệu minh họa.
4. Kiểm tra truy vấn xác minh bên dưới. Nếu có lỗi, dừng và đọc dòng lỗi trước khi tiếp tục.

## Trường hợp B — database `printkraft` đã có dữ liệu

Không chạy lại toàn bộ `printkraft_database_safe.sql` trên database đang dùng. Sao lưu và kiểm tra bản sao lưu trước khi thay schema.

### 1. Chỉ đọc để xác nhận máy chủ, database và schema nền

Chọn kết nối đích trong Navicat, mở cửa sổ truy vấn mới và chỉ chạy các câu đọc sau:

```sql
SELECT VERSION() AS server_version, DATABASE() AS selected_database;
SHOW CREATE TABLE printkraft.products;
SHOW CREATE TABLE printkraft.print_shops;
SHOW CREATE TABLE printkraft.quote_requests;
SHOW COLUMNS FROM printkraft.products;
SHOW COLUMNS FROM printkraft.print_shops;
SHOW COLUMNS FROM printkraft.quote_requests;
SHOW INDEX FROM printkraft.products;
```

Chỉ tiếp tục nếu `selected_database` là `printkraft`, máy chủ đúng là MariaDB dự kiến và các bảng/cột nền khớp code hiện tại. Nếu bảng thiếu, khóa chính có kiểu khác, cột nền như `products.is_active` không có, hoặc kết quả không rõ, dừng lại; không thử chạy toàn bộ file để “sửa cho đúng”.

### 2. Chạy riêng phần bổ sung đã rà soát

Sau khi backup và xác nhận các điều kiện trên, mở `printkraft_database_safe.sql`. Chỉ chọn từ tiêu đề `-- Idempotent schema additions for tables created by earlier versions.` đến hết file, rồi chạy phần được chọn trên kết nối đã xác nhận. Không chọn phần `CREATE DATABASE`/`CREATE TABLE` phía trên.

Phần này thêm các cột moderation cho sản phẩm, các trường hồ sơ xưởng và `quote_requests.seller_response_note`, bổ sung chỉ mục, rồi gán `APPROVED` cho sản phẩm cũ chưa có trạng thái kiểm duyệt. Câu backfill chỉ sửa `moderation_status`; không thay đổi `products.is_active`, nên sản phẩm đã ẩn vẫn giữ nguyên trạng thái ẩn. Sản phẩm mới có mặc định `PENDING_REVIEW`.

Lệnh `ALTER TABLE` có thể khóa bảng trong thời gian thực thi. Nếu công cụ báo lỗi về bảng/cột/kiểu dữ liệu, dừng tại đó; không chạy tiếp seed và không tự xóa hoặc đổi tên dữ liệu để vượt lỗi. Cảnh báo cho biết cột/chỉ mục cùng tên đã tồn tại có thể là bình thường, nhưng hãy kiểm tra định nghĩa bằng `SHOW COLUMNS`/`SHOW INDEX`: `IF NOT EXISTS` bỏ qua đối tượng hiện hữu chứ không sửa định nghĩa sai.

## Xác minh schema sau khi chạy

Các truy vấn sau chỉ đọc:

```sql
SELECT VERSION() AS server_version, DATABASE() AS selected_database;
SHOW COLUMNS FROM printkraft.print_shops LIKE 'contact_email';
SHOW COLUMNS FROM printkraft.quote_requests LIKE 'seller_response_note';
SHOW COLUMNS FROM printkraft.products LIKE 'moderation_status';
SHOW COLUMNS FROM printkraft.products LIKE 'rejection_reason';
SHOW COLUMNS FROM printkraft.products LIKE 'reviewed_by_user_id';
SHOW COLUMNS FROM printkraft.products LIKE 'reviewed_at';
SHOW INDEX FROM printkraft.products WHERE Key_name = 'idx_products_moderation_active';
SELECT moderation_status, is_active, COUNT(*) AS product_count
FROM printkraft.products
GROUP BY moderation_status, is_active
ORDER BY moderation_status, is_active;
```

`selected_database` phải là `printkraft`; các truy vấn `SHOW COLUMNS` phải trả về mỗi cột cần bổ sung và truy vấn nhóm phải chạy được. Nếu không, lưu nguyên văn thông báo lỗi và dừng để rà schema, không chạy lại liên tục.

## Nạp dữ liệu mẫu

Chỉ ở development/test, sau khi schema hợp lệ, xác nhận lại máy chủ và `printkraft`, sau đó chạy toàn bộ `printkraft_sample_data.sql`. Script dùng các email/slug/SKU/mã demo để tránh chèn lặp khi chạy lại; hàng hiện hữu, mật khẩu và trạng thái hiện hữu không bị ghi đè. Vì vậy, nếu email demo đã tồn tại, mật khẩu trong ghi chú đầu file chỉ dùng được khi nó trùng với mật khẩu đã có.

Dữ liệu bao gồm 5 chủ xưởng mẫu (4 xưởng `VERIFIED`, 1 xưởng `PENDING`), sản phẩm ở các trạng thái kiểm duyệt khác nhau, hai đơn, lịch sử, vận chuyển, báo giá, payment và review được đánh dấu demo. Không có file STL thật. Payment COD vẫn `PENDING`; giao dịch online mẫu là `FAILED`. Các bản ghi này không phải dữ liệu vận hành thật.

Nếu có lỗi trước `COMMIT`, dừng thực thi; khi phiên làm việc vẫn còn cùng kết nối và transaction chưa kết thúc, chạy `ROLLBACK`. Nếu không chắc transaction đã được rollback hay chưa, kiểm tra các marker `DEMO` trước khi thử lại.

## Mật khẩu tài khoản mới do seed tạo

- Admin: `admin@printkraft.vn` / `admin123`
- Customer: `minhtuan@gmail.com` / `123456`
- Workshop owners: `workshop01@example.test` đến `workshop05@example.test` / `Print3D-Demo-2026!`

Mật khẩu được lưu dạng PBKDF2. Seed không nâng quyền hoặc đặt lại mật khẩu của tài khoản đã tồn tại cùng email. Không dùng tài khoản hoặc dữ liệu demo trên production.

    