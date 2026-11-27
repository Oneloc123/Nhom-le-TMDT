# Cấu hình phí giao hàng GHN

Đặt các biến môi trường cho tiến trình Tomcat rồi khởi động lại máy chủ:

- `GHN_TOKEN` — token API của môi trường GHN đang dùng.
- `GHN_SHOP_ID` — mã cửa hàng thuộc tài khoản cấp token.
- `GHN_ENV` — mặc định `staging`. Dùng `production` cùng token và mã cửa hàng Production khi triển khai chính thức.

Checkout dùng API [Xem trước đơn của GHN](https://developer.ghn.vn/vi/docs/order/preview) để hỗ trợ địa chỉ hai cấp và địa chỉ cũ đã lưu. Điểm gửi là Đại học Nông Lâm, Linh Xuân, Thủ Đức, TP. Hồ Chí Minh. Mỗi đơn báo giá cho một kiện cố định 500 g, 20 × 15 × 10 cm.

Tổng thanh toán bằng tiền hàng cộng toàn bộ phí GHN. Phí được tính lại trên máy chủ khi khách xác nhận và được lưu cùng đơn. Khi GHN không báo được phí hoặc phí đã thay đổi, checkout yêu cầu khách kiểm tra lại trước khi tạo đơn.

## Tạo vận đơn và thử trên Staging

1. Chạy `migrate_ghn_delivery_workflow.sql` trên cơ sở dữ liệu PrintKraft đã có bảng vận đơn, lịch sử vận đơn, hỗ trợ và đổi trả. Sau đó triển khai bản WAR mới và khởi động lại Tomcat với cấu hình GHN Staging.
2. Tạo đơn mới qua checkout và xác nhận COD hoặc thanh toán. Trong **Đơn hàng** của xưởng, cập nhật tất cả sản phẩm còn hiệu lực đến **Sẵn sàng giao**. Tập kết kiện tại Đại học Nông Lâm rồi bấm **Tạo vận đơn GHN**. Gói khai báo mặc định là 500 g, 20 × 15 × 10 cm.
3. Khách mở **Đơn hàng của tôi → Theo dõi giao hàng** để xem mã GHN và hành trình. Tomcat truy vấn trạng thái GHN theo lô, với khoảng chờ ít nhất 2 phút giữa hai lần truy vấn cùng vận đơn. Có thể thử trên localhost với kết nối ra GHN Staging.
4. Khi PrintKraft ghi nhận GHN đã giao, trang theo dõi hiển thị hạn 72 giờ và hai thao tác xác nhận nhận hàng hoặc báo vấn đề. Nút báo vấn đề mở form khiếu nại đã chọn sẵn đơn. Sau khi gửi, ticket đang mở hoặc yêu cầu đổi trả chưa kết thúc sẽ chặn hoàn thành tự động. Nút xác nhận cũng tạm ẩn trong thời gian đó.

Mỗi đơn qua checkout sau khi áp dụng migration dùng một vận đơn chung. Dữ liệu vận đơn và hạn xác nhận nằm trong MySQL. Khi Tomcat khởi động lại, tác vụ nền tiếp tục kiểm tra các vận đơn đang giao và đơn đã đến hạn. Các đơn cũ tiếp tục dùng luồng cũ.

Với COD, GHN thu tổng tiền đã lưu của đơn (đã bao gồm phí giao hàng). Với đơn đã thanh toán, số tiền thu hộ bằng 0. PrintKraft chịu phí trực tiếp với GHN để tránh thu phí hai lần. Đối soát tiền thu hộ tiếp tục theo quy trình thanh toán của hệ thống.
