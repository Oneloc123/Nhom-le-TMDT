# Hướng dẫn cấu hình VNPAY Sandbox cho PrintKraft

Code đã tích hợp VNPAY Payment Gateway API 2.1.0. Khóa bí mật chỉ được đọc ở phía server,
không đặt trong source code và không gửi xuống trình duyệt.

## 1. Lấy tài khoản Sandbox

Đăng ký môi trường thử nghiệm tại <https://sandbox.vnpayment.vn/devreg/>. Sau khi VNPAY cấp
thông tin, bạn cần hai giá trị:

- `vnp_TmnCode`: mã website/terminal.
- `vnp_HashSecret`: khóa bí mật dùng để ký HMAC-SHA512.

## 2. Cập nhật database hiện có

Sau khi merge code tài khoản/đơn hàng ngày 04/11/2026 hoặc import file dump `printkraft.sql`
của nhóm, chọn đúng database rồi chạy `migrate_20261104_merged_schema.sql`. File này bổ sung
cả snapshot người nhận bị thiếu trong dump, trường tracking và trường đối soát VNPAY.

`migrate_vnpay_payment.sql` vẫn được giữ cho trường hợp database đã đầy đủ schema đặt hàng và
chỉ cần bổ sung riêng bốn trường VNPAY.

Ví dụ bằng MySQL CLI:

Mở file bằng MySQL Workbench, chọn đúng schema rồi chạy toàn bộ script. Nếu dùng Command Prompt:

```bat
mysql -u root -p printkraft < migrate_vnpay_payment.sql
```

## 3. Đặt biến môi trường trước khi chạy Tomcat

Trong chính cửa sổ PowerShell sẽ khởi động Tomcat/IDE:

```powershell
$env:PK_VNPAY_TMN_CODE="MA_TMN_VNPAY_CAP"
$env:PK_VNPAY_HASH_SECRET="KHOA_BI_MAT_VNPAY_CAP"
$env:PK_VNPAY_PAYMENT_URL="https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"
$env:PK_VNPAY_RETURN_URL="https://TEN-MIEN-CONG-KHA/TMDT_NhomLe/vnpay/return"
```

Khởi động lại Tomcat sau khi đổi cấu hình. Có thể dùng Java system properties tương ứng:
`pk.vnpay.tmnCode`, `pk.vnpay.hashSecret`, `pk.vnpay.paymentUrl`, `pk.vnpay.returnUrl`.

Nếu chạy bằng Smart Tomcat trong IntelliJ IDEA, mở **Run > Edit Configurations > Smart Tomcat**,
thêm bốn biến trên vào **Environment variables**, Apply rồi khởi động lại cấu hình. Đặt biến trong
một cửa sổ PowerShell khác sẽ không tự truyền vào IntelliJ đang mở sẵn.

## 4. Khai báo IPN với VNPAY

Trong cấu hình merchant Sandbox, đặt IPN URL là:

```text
https://TEN-MIEN-CONG-KHA/TMDT_NhomLe/vnpay/ipn
```

VNPAY gọi IPN từ server của họ, vì vậy `localhost` không nhận được callback này. Khi phát triển
trên máy cá nhân, hãy dùng một HTTPS tunnel công khai (ví dụ ngrok hoặc Cloudflare Tunnel) và
thay `TEN-MIEN-CONG-KHA` bằng địa chỉ tunnel. Nếu context path khi deploy không phải
`TMDT_NhomLe`, hãy thay nó trong cả Return URL và IPN URL.

## 5. Chạy thử

1. Tạo đơn hàng và đến bước **Thanh toán**.
2. Chọn **Thanh toán qua VNPAY**.
3. Hoàn tất bằng thông tin thẻ thử nghiệm do VNPAY Sandbox cung cấp.
4. Kiểm tra trang kết quả và lịch sử mua hàng.
5. Kiểm tra bảng `payments`: giao dịch thành công phải có `status = 'PAID'`, mã giao dịch,
   mã phản hồi và thông tin ngân hàng.

Trang `/vnpay/return` chỉ hiển thị kết quả cho khách hàng. Đơn hàng chỉ được đánh dấu đã thanh
toán tại `/vnpay/ipn` sau khi server xác minh chữ ký, số tiền và trạng thái giao dịch.

## Khi chuyển sang production

- Dùng TMN Code, Hash Secret và Payment URL do VNPAY production cấp.
- Cấu hình Return/IPN bằng tên miền HTTPS thật.
- Không commit Hash Secret vào Git, file JSP, JavaScript hoặc file cấu hình được public.
- Thử đầy đủ các tình huống: thành công, hủy, hết hạn, sai số tiền và IPN gửi lặp.
