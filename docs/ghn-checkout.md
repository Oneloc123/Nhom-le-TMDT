# Cấu hình phí giao hàng GHN

Đặt các biến môi trường cho tiến trình Tomcat rồi khởi động lại máy chủ:

- `GHN_TOKEN` — token API của môi trường GHN đang dùng.
- `GHN_SHOP_ID` — mã cửa hàng thuộc tài khoản cấp token.
- `GHN_ENV` — mặc định `staging`. Dùng `production` cùng token và mã cửa hàng Production khi triển khai chính thức.

Checkout dùng API [Xem trước đơn của GHN](https://developer.ghn.vn/vi/docs/order/preview) để hỗ trợ địa chỉ hai cấp và địa chỉ cũ đã lưu. Điểm gửi là Đại học Nông Lâm, Linh Xuân, Thủ Đức, TP. Hồ Chí Minh. Mỗi đơn báo giá cho một kiện cố định 500 g, 20 × 15 × 10 cm.

Tổng thanh toán bằng tiền hàng cộng toàn bộ phí GHN. Phí được tính lại trên máy chủ khi khách xác nhận và được lưu cùng đơn. Khi GHN không báo được phí hoặc phí đã thay đổi, checkout yêu cầu khách kiểm tra lại trước khi tạo đơn.
