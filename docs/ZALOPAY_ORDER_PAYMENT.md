# Thanh toán đơn hàng bằng ví ZaloPay

Luồng thanh toán đơn hàng sử dụng lại tích hợp ZaloPay OpenAPI v2 đang phục vụ đặt cọc yêu cầu báo giá và in 3D.

## Phạm vi

- Áp dụng cho đơn hàng bán lẻ ở trạng thái `Chờ thanh toán`.
- Thu toàn bộ `orders.total_amount`; không áp dụng tỷ lệ đặt cọc 30%.
- Ghi giao dịch vào bảng `payments` với `provider = 'ZALOPAY'`, `payment_method = 'ZaloPay'`, mã `app_trans_id` trong `provider_transaction_ref`, URL thanh toán trong `checkout_url`.
- Đơn hàng chỉ chuyển sang `Đã xác nhận` / `Đã thanh toán` sau khi xác minh callback hoặc truy vấn trạng thái từ ZaloPay, đối chiếu đúng số tiền và cập nhật trong transaction.
- Giao dịch đến muộn cho đơn đã thanh toán/hủy được đánh dấu `REVIEW_REQUIRED` để đối soát, không tự động tạo phân bổ tài chính lần hai.

Không cần migration riêng nếu database đã có schema `payments` trong `printkraft_database_safe.sql` hoặc schema đã được cập nhật theo migration VNPAY hiện tại. Các bảng `quote_payments` và `custom_print_payments` của đặt cọc không bị thay đổi.

## Cấu hình

ZaloPay Sandbox dùng cấu hình hiện tại của dự án:

- `PK_ZALOPAY_APP_ID`
- `PK_ZALOPAY_KEY1`
- `PK_ZALOPAY_KEY2`
- `PK_ZALOPAY_CREATE_URL` (mặc định `https://sb-openapi.zalopay.vn/v2/create`)
- `PK_ZALOPAY_QUERY_URL` (mặc định `https://sb-openapi.zalopay.vn/v2/query`)
- `PK_ZALOPAY_CALLBACK_URL` (URL HTTPS công khai tới `/zalopay/callback`, khuyến nghị cấu hình)
- `PK_ZALOPAY_REDIRECT_URL` (URL HTTPS công khai tới `/zalopay/return`, dùng để Gateway quay về website)

### Chạy local với HTTPS tunnel

Không nên để ZaloPay Gateway redirect về `http://localhost`. Tạo HTTPS tunnel tới cổng Tomcat (ví dụ `ngrok http 8080`), sau đó:

1. Mở PrintKraft bằng chính URL HTTPS của tunnel và đăng nhập trên URL đó trước khi đặt hàng. Không bắt đầu thanh toán ở `localhost` rồi mong cookie đăng nhập được chia sẻ sang domain tunnel.
2. Trong Environment variables của Smart Tomcat (IntelliJ), khai báo:
   - `PK_ZALOPAY_REDIRECT_URL=https://<domain-tunnel>/<context-path>/zalopay/return`
   - `PK_ZALOPAY_CALLBACK_URL=https://<domain-tunnel>/<context-path>/zalopay/callback`
3. Thay `<context-path>` bằng context path thực tế khi deploy (ví dụ `TMDT_NhomLe`), Apply và khởi động lại Tomcat.
4. Mở lại website qua URL tunnel, tạo đơn và thanh toán bằng ZaloPay.

Code ưu tiên `PK_ZALOPAY_REDIRECT_URL` đã cấu hình, kể cả khi request ban đầu có host localhost. Nếu không cấu hình, hệ thống mới tự xây URL return từ host hiện tại.

Có thể khai báo Java system properties tương ứng `pk.zalopay.*`. Không commit merchant keys production vào repository.

## Kiểm thử

1. Tạo một đơn hàng mới, giữ trạng thái `Chờ thanh toán`.
2. Chọn **Ví điện tử ZaloPay** tại trang thanh toán.
3. Hoàn tất hoặc hủy giao dịch trong ZaloPay Sandbox.
4. Xác minh bản ghi `payments`: provider `ZALOPAY`, đúng số tiền và trạng thái `PAID` / `FAILED`.
5. Với giao dịch `PAID`, kiểm tra đơn chuyển sang `Đã xác nhận`, payment status là `Đã thanh toán` và phân bổ tài chính chỉ được tạo một lần.
6. Thử callback lặp, sai chữ ký, sai số tiền và giao dịch đến sau khi đơn đã được thanh toán/hủy.

Trên localhost ZaloPay không thể gọi callback URL nội bộ; luồng Return sẽ truy vấn `/v2/query`. Khi kiểm thử IPN, dùng HTTPS tunnel hoặc domain công khai.
