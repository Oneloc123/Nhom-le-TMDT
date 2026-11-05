# Admin MVP change log

## 2026-10-21 17:38 Asia/Ho_Chi_Minh

- **Người thực hiện:** Codex (AI assistant).
- **Use case:** Hoàn thiện hướng dẫn vận hành hai file SQL; phân biệt rõ khởi tạo database trống với bổ sung schema cho database đang có.
- **File thay đổi:** `printkraft_database_safe.sql`, `printkraft_sample_data.sql`, `docs/DATABASE_SETUP.md`, `docs/ADMIN_CHANGELOG.md`.
- **Nội dung / lý do:** Thêm `USE printkraft` vào khối bổ sung có thể chạy riêng; seed có truy vấn kiểm tra marker cho đủ 22 bảng và chỉ gắn người duyệt nếu tài khoản mẫu thực sự là Admin đang hoạt động. Hướng dẫn database hiện hữu yêu cầu backup, preflight chỉ đọc và chỉ chọn khối bổ sung; không khuyên chạy lại toàn bộ bootstrap.
- **Kiểm thử:** Kiểm tra tĩnh khớp 22/22 bảng schema, 22/22 bảng seed, 22/22 dòng xác minh và tên cột của cả 76 câu `INSERT`; seed không có DDL/UPDATE/DELETE/DROP/TRUNCATE; schema không có DROP/TRUNCATE/DELETE. `git diff --check` đạt. Không kết nối hoặc chạy SQL, không chạy kiểm thử trên MariaDB.
- **Giới hạn:** Cần người vận hành chạy thủ công trên đúng database sau khi kiểm tra/backup; không thể xác nhận kết quả runtime nếu chưa thực thi trên máy chủ đích.

## 2026-10-21 17:33 Asia/Ho_Chi_Minh

- **Người thực hiện:** Codex (AI assistant).
- **Use case:** Dễ cài đặt lại schema hiện hành và có dữ liệu phát triển liên kết đầy đủ để thử marketplace/Admin/Workshop.
- **File thay đổi / thêm / xóa:** `printkraft_database_safe.sql`; `printkraft_sample_data.sql`; xóa `printkraft_workshop_sample_data.sql`; cập nhật `docs/DATABASE_SETUP.md`, `docs/WORKSHOP_PORTAL.md`, `docs/ADMIN_CHANGELOG.md`.
- **Nội dung / lý do:** Schema dùng `CREATE TABLE IF NOT EXISTS`, thêm cột/index mới bằng cú pháp điều kiện cho MariaDB 10.4.32 và backfill sản phẩm cũ chưa có trạng thái thành `APPROVED` mà không đổi `is_active`; mặc định sản phẩm mới vẫn `PENDING_REVIEW`. Seed được hợp nhất thành một file, dùng marker để tránh chèn lặp, bao gồm mẫu liên kết cho toàn bộ 22 bảng.
- **Ghi chú dữ liệu:** Seed chỉ dành cho dev/test; tất cả giao dịch, tệp, đánh giá, giá và hồ sơ là demo. Không chạy SQL trên DB trong lần thay đổi này.
- **Kiểm thử:** Rà cấu trúc theo 22 `CREATE TABLE` trong schema và các bảng do seed tham chiếu; rà cú pháp các nhánh `IF NOT EXISTS` theo tài liệu MariaDB. Chưa kết nối hoặc chạy trên MariaDB thật; chưa chạy kiểm thử SQL bằng server.
- **Chưa hoàn thành / giới hạn:** `IF NOT EXISTS` không sửa định nghĩa hiện hữu sai kiểu và không kiểm tra khóa/index cùng tên có cấu trúc khác; người vận hành vẫn cần backup và preflight trước khi chạy. Đóng gói/kiểm thử ứng dụng không thuộc thay đổi SQL này.

## 2026-10-21 17:06 Asia/Ho_Chi_Minh

- **Người thực hiện:** Codex (AI assistant).
- **Use case:** Chuẩn bị dữ liệu phát triển cho năm xưởng 3D để kiểm tra Workshop Portal.
- **File thay đổi / thêm:** `printkraft_workshop_sample_data.sql`; `docs/WORKSHOP_PORTAL.md`; `docs/ADMIN_CHANGELOG.md`.
- **Nội dung / lý do:** Thêm seed SQL chỉ chèn năm tài khoản thường và năm shop gắn đúng chủ sở hữu. Các shop bắt đầu ở `PENDING`, rating bằng `0`; không cấp quyền Admin và không sửa/xóa bản ghi có sẵn. Chạy lại bỏ qua email, chủ sở hữu hoặc slug đã tồn tại. Ghi hướng dẫn đăng nhập demo và xác minh shop.
- **Database:** Không kết nối hoặc chạy SQL trên database. Người dùng cần chạy thủ công file `printkraft_workshop_sample_data.sql` trên database development `printkraft` sau khi xác nhận schema cơ bản.
- **Kiểm thử:** Rà soát nội dung SQL theo schema `users` và `print_shops` trong `printkraft_database_safe.sql`; chưa thực thi SQL, chưa kiểm thử đăng nhập hoặc database.
- **Chưa hoàn thành:** Chưa có sản phẩm, đơn hàng hoặc yêu cầu báo giá mẫu; chưa xác nhận runtime database. Năm shop cần được Admin xác minh trước khi xuất hiện công khai.

## 2026-10-21 16:58 Asia/Ho_Chi_Minh

- **Người thực hiện:** Codex (AI assistant).
- **Use case:** Thêm Workshop Portal cho chủ xưởng hiện có, dùng dữ liệu thật theo quan hệ `print_shops.owner_user_id`, đồng thời giữ nguyên luồng customer/Admin và không tự sửa database.
- **File thay đổi / thêm:**
  - Backend: `src/main/java/vn/edu/hcmuaf/fit/tmdt_nhomle/WorkshopRouterServlet.java`; `src/main/java/vn/edu/hcmuaf/fit/tmdt_nhomle/controller/workshop/WorkshopApiServlet.java`; `src/main/java/vn/edu/hcmuaf/fit/tmdt_nhomle/controller/workshop/WorkshopApplicationServlet.java`; `src/main/java/vn/edu/hcmuaf/fit/tmdt_nhomle/dao/WorkshopDao.java`; `src/main/java/vn/edu/hcmuaf/fit/tmdt_nhomle/model/WorkshopWorkflow.java`; `src/main/java/vn/edu/hcmuaf/fit/tmdt_nhomle/service/PortalRedirect.java`.
  - Giao diện: `src/main/webapp/WEB-INF/workshop/index.jsp`; `src/main/webapp/WEB-INF/workshop/apply.jsp`; `src/main/webapp/workshop-ui/workshop.js`; `src/main/webapp/workshop-ui/workshop.css`.
  - Tests/docs/schema: `src/test/java/vn/edu/hcmuaf/fit/tmdt_nhomle/WorkshopRouterTest.java`; `src/test/java/vn/edu/hcmuaf/fit/tmdt_nhomle/model/WorkshopWorkflowTest.java`; `src/test/java/vn/edu/hcmuaf/fit/tmdt_nhomle/security/AuthFilterTest.java`; `docs/WORKSHOP_PORTAL.md`; `docs/DATABASE_SETUP.md`; `docs/ADMIN_CHANGELOG.md`; `printkraft_database_safe.sql` (index ảnh chính và lệnh bổ sung thủ công).
- **Nội dung / lý do:** Thêm đăng ký xưởng chờ Admin xác minh; dashboard không có doanh thu giả; hồ sơ/năng lực, offer/giá, sản phẩm có moderation tách khỏi `is_active`, báo giá, đơn hàng theo shop, lịch sử trạng thái/tiến độ, và đánh giá chỉ đọc. Seller không thay đổi trạng thái thanh toán/đơn toàn cục. Catalog, cửa hàng và checkout công khai chỉ dùng shop đã xác minh và đang hoạt động. Mọi Workshop POST cần CSRF; `AuthFilter` làm mới trạng thái tài khoản; DAO kiểm tra chủ sở hữu ở mỗi truy vấn.
- **Schema:** Không có migration runner, không thêm dependency và không chạy DDL/UPDATE trên database. `printkraft_database_safe.sql` chứa schema đầy đủ cho database mới và SQL bổ sung thủ công cho database hiện hữu; lần này thêm index tra ảnh chính và câu lệnh bổ sung tương ứng. Hướng dẫn Navicat/preflight nằm trong `docs/DATABASE_SETUP.md`.
- **Kiểm thử:** PowerShell `$env:PK_DB_NAME='printkraft'; .\mvnw.cmd clean test` — BUILD SUCCESS, 41 tests, 0 failures, 0 errors, 0 skipped. Smoke test chỉ đọc đã cho thấy schema đang dùng thiếu `print_shops.contact_email` (Unknown column 1054); `ShopDao` nay đọc các cột workshop mới theo metadata để trang tìm xưởng customer không lỗi trước khi bổ sung schema. Không chạy SQL ghi. `node --check src/main/webapp/workshop-ui/workshop.js` — đạt. PowerShell `$env:PK_DB_NAME='printkraft'; .\mvnw.cmd -DskipTests package` — BUILD SUCCESS, WAR tại `target/TMDT_NhomLe-1.0-SNAPSHOT.war`. `git diff --check` — đạt.
- **Chưa hoàn thành / bước thủ công:** Chưa chạy Tomcat hoặc kiểm tra HTTP/browser thực tế. Workshop profile, đăng ký, sản phẩm và phản hồi báo giá cần schema workshop hiện diện; trên database đang có cần kiểm tra cấu trúc thật rồi chạy thủ công đúng các câu trong khối `MANUAL WORKSHOP PORTAL ADDITIONS FOR AN EXISTING DATABASE` của `printkraft_database_safe.sql` và phần sản phẩm nếu còn thiếu. Thiếu `contact_email` đã được kiểm chứng; chưa xác nhận trạng thái các cột/index khác. File design riêng chỉ hiện tên gốc, chưa có upload/download được bảo vệ vì ứng dụng chưa có storage contract.

## 2026-10-21 15:42 Asia/Ho_Chi_Minh

- **Người thực hiện:** Codex (AI assistant).
- **Use case:** Chuyển cách quản lý schema sang SQL thủ công, dễ kiểm soát; xử lý trước nguyên nhân HTTP 500 mà không thay đổi database thật.
- **File thay đổi / thêm / xóa:** `printkraft_database_safe.sql`; `docs/DATABASE_SETUP.md`; `docs/ADMIN_CHANGELOG.md`; xóa `database/migrations/20261021_01_product_moderation.sql` và `docs/ADMIN_MIGRATIONS.md`.
- **Nội dung / lý do:** Giữ schema khởi tạo đầy đủ cho database mới trong `printkraft_database_safe.sql`, gồm `view_count` và các trường kiểm duyệt. Thêm khối SQL bổ sung cho database hiện có ở dạng comment để không tự chạy khi import toàn file; hướng dẫn preflight chỉ đọc, điều kiện phải dừng, thứ tự chạy thủ công và truy vấn xác minh được ghi trong `docs/DATABASE_SETUP.md`. Sản phẩm cũ được backfill thành `APPROVED` mà không đổi `is_active`; sản phẩm mới nhận mặc định `PENDING_REVIEW`. Không tìm thấy Flyway, Liquibase hoặc mã tự chạy DDL trong ứng dụng.
- **HTTP 500:** Log Tomcat do người dùng cung cấp cho thấy `Access denied for user 'root'@'localhost'` trong `Database.openNew()` khi trang chủ gọi `ProductDao.getBestsellers()`. Đây là lỗi xác thực cấu hình kết nối, xảy ra trước khi chạy truy vấn; log này không chứng minh thiếu cột. Chưa có thông tin xác nhận migration đã chạy, và không kết nối hay thay đổi database thật.
- **Kiểm thử:** `mvnw clean test` — BUILD SUCCESS, 25 tests, 0 failures, 0 errors, 1 skipped (kiểm tra MySQL không kết nối được do Access denied). Node `--check` — đạt với 6 file JavaScript Admin. `mvnw -DskipTests package` — BUILD SUCCESS, WAR được tạo. `git diff --check` — đạt; Git phát cảnh báo chuẩn hóa LF/CRLF trên một số file đã có thay đổi.
- **Chưa hoàn thành / bước thủ công:** Chưa chạy SQL bổ sung và chưa xác minh schema thực tế; chưa chạy ứng dụng/Tomcat để chứng minh HTTP 500 đã hết. Theo `docs/DATABASE_SETUP.md`: trên DB mới dùng `printkraft_database_safe.sql`; trên DB đang dùng tuyệt đối không chạy toàn file, chỉ xem schema và cân nhắc từng lệnh trong khối comment sau khi kiểm tra đúng DB, backup và đủ điều kiện. Cần cập nhật thông tin đăng nhập MySQL hợp lệ trong cấu hình chạy Tomcat/IDE rồi khởi động lại và kiểm tra URL bị lỗi.
- **Ghi chú lịch sử:** Các mục cũ bên dưới mô tả tình trạng trước thay đổi này. Riêng hướng dẫn chạy migration ở mục `15:58` đã bị thay thế; không chạy file migration đã nêu ở đó.

## 2026-10-21 14:38 Asia/Ho_Chi_Minh

- **Người thực hiện:** Codex (AI assistant).
- **Use case:** Hợp nhất Admin thành năm mục Dashboard, Người dùng, Seller / Xưởng in, Sản phẩm, Đơn hàng; dùng dữ liệu MySQL qua API có kiểm soát quyền; giữ trạng thái nhất quán sau khi tải lại.
- **Nội dung:** Thêm `AdminDao` và `/admin/api/*` cho số liệu Dashboard, tìm kiếm/lọc/phân trang, chi tiết và thao tác hồ sơ người dùng, xưởng, kiểm duyệt sản phẩm, đơn hàng và yêu cầu báo giá. Mọi POST Admin cần CSRF; `AuthFilter` tải lại tài khoản từ DB để xác minh quyền/trạng thái. Chuyển đăng nhập Admin về `/admin`, thống nhất các alias cũ vào SPA. Chuyển trạng thái đơn và ghi lịch sử trong cùng transaction. Trạng thái kiểm duyệt sản phẩm tách biệt `is_active`; duyệt không tự bật sản phẩm đang ẩn. Catalog, giỏ hàng và yêu cầu báo giá chỉ nhận sản phẩm đã duyệt và đang hoạt động. Dashboard không hiển thị số liệu tài chính giả.
- **File thay đổi / thêm:**
  - `src/main/java/vn/edu/hcmuaf/fit/tmdt_nhomle/AdminRouterServlet.java`
  - `src/main/java/vn/edu/hcmuaf/fit/tmdt_nhomle/controller/admin/AdminApiServlet.java`
  - `src/main/java/vn/edu/hcmuaf/fit/tmdt_nhomle/controller/admin/AdminDashboardServlet.java`
  - `src/main/java/vn/edu/hcmuaf/fit/tmdt_nhomle/controller/admin/UserManagementServlet.java`
  - `src/main/java/vn/edu/hcmuaf/fit/tmdt_nhomle/controller/customer/LoginServlet.java`
  - `src/main/java/vn/edu/hcmuaf/fit/tmdt_nhomle/dao/AdminDao.java`
  - `src/main/java/vn/edu/hcmuaf/fit/tmdt_nhomle/dao/CartDao.java`
  - `src/main/java/vn/edu/hcmuaf/fit/tmdt_nhomle/dao/ProductDao.java`
  - `src/main/java/vn/edu/hcmuaf/fit/tmdt_nhomle/dao/QuoteDao.java`
  - `src/main/java/vn/edu/hcmuaf/fit/tmdt_nhomle/model/AdminAccountPolicy.java`
  - `src/main/java/vn/edu/hcmuaf/fit/tmdt_nhomle/model/ProductModeration.java`
  - `src/main/java/vn/edu/hcmuaf/fit/tmdt_nhomle/security/AuthFilter.java`
  - `src/main/java/vn/edu/hcmuaf/fit/tmdt_nhomle/util/Csrf.java`
  - `src/main/java/vn/edu/hcmuaf/fit/tmdt_nhomle/util/Json.java`
  - `src/main/webapp/WEB-INF/admin/index.jsp`
  - `src/main/webapp/admin-ui/assets/admin.css`
  - `src/main/webapp/admin-ui/js/api.js`
  - `src/main/webapp/admin-ui/js/app.js`
  - `src/main/webapp/admin-ui/js/layout.js`
  - `src/main/webapp/admin-ui/js/pages/dashboard.js`
  - `src/main/webapp/admin-ui/js/pages/management.js`
  - Xóa JSP giao diện Admin cũ: `src/main/webapp/admin/dashboard.jsp`, `src/main/webapp/admin/user-management.jsp`, `src/main/webapp/admin/header.jsp`, `src/main/webapp/admin/sidebar.jsp`.
  - Xóa module mock không còn được import: `src/main/webapp/admin-ui/js/data.js`, `src/main/webapp/admin-ui/js/pages/finance.js`, `src/main/webapp/admin-ui/js/pages/insights.js`, `src/main/webapp/admin-ui/js/pages/settings.js`.
  - Thêm kiểm thử `src/test/java/vn/edu/hcmuaf/fit/tmdt_nhomle/AdminRouterTest.java`, `src/test/java/vn/edu/hcmuaf/fit/tmdt_nhomle/model/AdminAccountPolicyTest.java`, `src/test/java/vn/edu/hcmuaf/fit/tmdt_nhomle/model/ProductModerationTest.java`, `src/test/java/vn/edu/hcmuaf/fit/tmdt_nhomle/security/AuthFilterTest.java`, `src/test/java/vn/edu/hcmuaf/fit/tmdt_nhomle/util/JsonTest.java`; cập nhật assertion URL JDBC trong `src/test/java/vn/edu/hcmuaf/fit/tmdt_nhomle/db/DbConfigTest.java` để kiểm tra các thuộc tính cấu hình thay vì thứ tự tham số.
- **Database / migration:** Chưa sửa schema và chưa tạo/chạy migration. Hai schema SQL đang có chưa chứa các cột kiểm duyệt cần thiết (`moderation_status`, `rejection_reason`, `reviewed_by_user_id`, `reviewed_at`). Cấu hình runtime hiện không xác thực được vào MySQL; chưa đọc được `SELECT VERSION(), DATABASE()` và chưa xác nhận DB đích/bản sao lưu. Chờ xác minh phiên bản MySQL và target trước khi tạo migration tương thích. Cho tới khi migration được lập, các truy vấn catalog/Admin có dùng trạng thái kiểm duyệt chưa thể chạy trên schema hiện tại.
- **Kiểm thử:** `mvnw clean test` — BUILD SUCCESS, 25 tests, 0 failures, 1 skipped. Kiểm thử kết nối MySQL bị skip vì xác thực kết nối local thất bại; không có integration test DB thật. Kiểm tra cú pháp các module JavaScript Admin bằng Node — OK. `mvnw -DskipTests package` — BUILD SUCCESS; WAR chứa SPA và `AdminApiServlet`/`AdminDao`, không chứa JSP Admin cũ đã xóa. `git diff --check` — không phát hiện lỗi whitespace (Git chỉ đưa cảnh báo chuẩn hóa LF/CRLF).
- **Chưa hoàn thành / bước thủ công:** Chưa thể nghiệm thu thao tác CRUD/trạng thái trên MySQL thật, xác minh session với trạng thái tài khoản đã khóa trong DB, hoặc chạy migration. Cần cung cấp kết quả `SELECT VERSION(), DATABASE()` từ kết nối chỉ đọc tới DB đích; sao lưu DB và xác nhận rõ target trước khi chạy migration. Sau migration cần chạy lại integration test với DB đã xác nhận và kiểm tra thủ công các luồng Admin/customer.
- **Ghi chú:** Không chạy SQL thay đổi dữ liệu. Giữ `AdminDashboardServlet` và `UserManagementServlet` ở vai trò alias tương thích; cả hai dùng cùng giao diện mới, không còn JSP Admin cạnh tranh.

## 2026-10-21 15:58 Asia/Ho_Chi_Minh

- **Người thực hiện:** Codex (AI assistant).
- **Use case:** Chuẩn bị lưu trạng thái kiểm duyệt sản phẩm an toàn cho DB đã xác nhận là `printkraft` trên MariaDB 10.4.32.
- **File thay đổi / thêm:** `printkraft_database_safe.sql`, `database/migrations/20261021_01_product_moderation.sql`, `docs/ADMIN_MIGRATIONS.md`, `docs/ADMIN_CHANGELOG.md`.
- **Nội dung / lý do:** Cập nhật schema an toàn dùng cho cài mới với trạng thái mặc định `PENDING_REVIEW`; migration nâng cấp hiện hữu thêm trạng thái, lý do từ chối, Admin duyệt, thời điểm duyệt và index. Bản ghi cũ được backfill thành `APPROVED`, không sửa `is_active`, nên sản phẩm đã ẩn không bị bật lại. Không sửa bản SQL gốc có lệnh xóa database.
- **Tương thích:** MariaDB 10.4.32 hỗ trợ cú pháp `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` và `CREATE INDEX IF NOT EXISTS` dùng trong migration theo [tài liệu ALTER TABLE](https://mariadb.com/docs/server/reference/sql-statements/data-definition/alter/alter-table) và [tài liệu CREATE INDEX](https://mariadb.com/docs/server/reference/sql-statements/data-definition/create/create-index). Đây là rà soát cú pháp; chưa chạy trên máy chủ DB.
- **Kiểm thử:** Chưa chạy migration và chưa chạy integration test DB. Kiểm thử Java gần nhất: `mvnw clean test` — BUILD SUCCESS, 25 tests, 0 failures, 1 skipped do không đăng nhập được MySQL. Đóng gói WAR gần nhất — BUILD SUCCESS.
- **Còn chờ / bước thủ công:** Chưa nhận xác nhận bản sao lưu. Không chạy migration cho tới khi có backup đã kiểm tra. Khi sẵn sàng, đưa ứng dụng vào maintenance, chạy đúng migration trên DB `printkraft`, kiểm tra kết quả `SELECT` cuối script, rồi chạy lại integration test và luồng catalog/Admin.
