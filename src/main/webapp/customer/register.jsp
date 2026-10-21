<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng ký — PrintKraft 3D</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/bootstrap.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth.css">
</head>
<body class="auth-body" data-pk-ctx="${pageContext.request.contextPath}">

<div class="auth-shell">
    <aside class="auth-hero">
        <a class="brand" href="${pageContext.request.contextPath}/"><span class="cube"></span> PrintKraft <b>3D</b></a>
        <h1>Tạo tài khoản, <span class="accent">bắt đầu in.</span></h1>
        <p>Lưu địa chỉ nhận bản in, theo dõi đơn <b>.stl</b> / <b>.obj</b> đang in theo thời gian thực
            và nhận báo giá FDM / SLA theo trọng lượng vật liệu.</p>
        <ul class="hero-points">
            <li>Báo giá PLA · ABS · Resin theo gram</li>
            <li>Ước tính thời gian in trước khi đặt</li>
            <li>Theo dõi trạng thái: Mới → Đang in → Hoàn tất → Đã giao</li>
        </ul>
    </aside>

    <main class="auth-card">
        <c:if test="${not empty error}">
            <div class="alert alert-err" role="alert"><c:out value="${error}"/></div>
        </c:if>

        <%-- ============ Bước 1: form đăng ký ============ --%>
        <div data-pk-step="form" <c:if test="${step == 'verify'}">hidden</c:if>>
            <h2>Đăng ký</h2>
            <p class="sub">Tài khoản khách hàng đặt in 3D PrintKraft.</p>

            <form method="post" action="${pageContext.request.contextPath}/register"
                  class="stack-form" data-pk-form="register" novalidate>
                <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                <label>Họ và tên
                    <input type="text" name="fullName" placeholder="Nguyễn Văn A"
                           maxlength="120" autocomplete="name" required>
                </label>
                <label>Email
                    <input type="email" name="email" placeholder="vidu@gmail.com"
                           maxlength="190" autocomplete="email" required>
                </label>
                <div class="form-row-2">
                    <label>Số điện thoại
                        <input type="text" name="phone" inputmode="numeric" placeholder="09xx xxx xxx"
                               maxlength="12" autocomplete="tel">
                    </label>
                    <label>Mật khẩu
                        <span class="pass-field">
                            <input type="password" name="password" id="regPassword"
                                   placeholder="Tối thiểu 6 ký tự" autocomplete="new-password" required>
                            <button type="button" class="pass-toggle" data-pk-toggle-pass="regPassword"
                                    aria-pressed="false" aria-label="Hiện/ẩn mật khẩu">Hiện</button>
                        </span>
                    </label>
                </div>
                <label>Xác nhận mật khẩu
                    <span class="pass-field">
                        <input type="password" name="confirm" id="regConfirm"
                               placeholder="Nhập lại mật khẩu" autocomplete="new-password" required>
                        <button type="button" class="pass-toggle" data-pk-toggle-pass="regConfirm"
                                aria-pressed="false" aria-label="Hiện/ẩn mật khẩu">Hiện</button>
                    </span>
                </label>
                <label>Địa chỉ nhận bản in
                    <input type="text" name="address" maxlength="255"
                           placeholder="12 Nguyễn Huệ, Q.1, TP.HCM">
                </label>
                <button type="submit" class="btn-primary btn-block">Tiếp tục — nhận OTP demo</button>
            </form>

            <div class="divider"><span>hoặc</span></div>

            <div class="social-row">
                <button type="button" class="btn-social btn-google" data-pk-social="google"
                        aria-haspopup="dialog">
                    <span class="g" aria-hidden="true">G</span> Đăng ký bằng Google
                </button>
                <button type="button" class="btn-social btn-facebook" data-pk-social="facebook"
                        aria-haspopup="dialog">
                    <span class="f" aria-hidden="true">f</span> Đăng ký bằng Facebook
                </button>
            </div>
            <p class="social-demo-hint">Nút mạng xã hội đang ở chế độ demo — chưa kết nối OAuth thật.</p>

            <p class="switch">Đã có tài khoản?
                <a class="link" href="${pageContext.request.contextPath}/login">Đăng nhập</a>
            </p>
        </div>

        <%-- ============ Bước 2: xác thực OTP demo ============ --%>
        <div data-pk-step="verify" <c:if test="${step != 'verify'}">hidden</c:if>>
            <div class="otp-head">
                <h2>Xác thực OTP</h2>
                <div class="demo-badge" title="Chế độ demo">DEMO</div>
            </div>
            <p class="sub">Nhập mã xác thực 6 số cho
                <b data-pk-verify-email><c:out value="${email}"/></b>.</p>

            <div class="demo-note" data-pk-verify-note>
                <b>Chế độ demo:</b> mã OTP kiểm thử là 111111; hệ thống chưa gửi email thật.
            </div>

            <form method="post" action="${pageContext.request.contextPath}/register"
                  class="stack-form" data-pk-form="register-verify">
                <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                <input type="hidden" name="action" value="verify">
                <input type="hidden" name="email" value="<c:out value='${email}'/>"
                       data-pk-verify-bound autocomplete="off">
                <label>Mã OTP
                    <input type="text" name="code" inputmode="numeric" pattern="[0-9]{6}"
                           maxlength="6" placeholder="111111" autocomplete="one-time-code" autofocus required>
                </label>
                <button type="submit" class="btn-primary btn-block">Xác nhận và tạo tài khoản</button>
            </form>

            <p class="switch">Nhập sai email?
                <a class="link" href="${pageContext.request.contextPath}/register">Đăng ký lại</a>
            </p>
        </div>

        <%-- ============ Bước 3: thành công ============ --%>
        <div data-pk-step="success" hidden>
            <h2>Đăng ký thành công 🎉</h2>
            <p class="sub" data-pk-success-text>
                Tài khoản đã được kích hoạt. Đang chuyển hướng tới trang đăng nhập…
            </p>
            <a class="btn-primary btn-block" style="text-align:center;"
               href="${pageContext.request.contextPath}/login">Đi tới trang đăng nhập</a>
        </div>
    </main>
</div>

<%-- Modal thông báo OAuth chưa cấu hình (chỉ demo giao diện) --%>
<div class="pk-modal" id="socialDemoModal" role="dialog" aria-modal="true"
     aria-labelledby="socialDemoTitle" hidden>
    <div class="pk-modal-backdrop"></div>
    <div class="pk-modal-card">
        <div class="pk-modal-head">
            <h3 id="socialDemoTitle">Đăng nhập <span data-pk-social-name>Google</span> — chế độ demo</h3>
            <button type="button" class="pk-modal-x" data-pk-modal-close aria-label="Đóng">&times;</button>
        </div>
        <div class="demo-badge-row">
            <span class="demo-badge">DEMO</span>
            <span class="demo-badge demo-badge-off">OAuth: chưa cấu hình</span>
        </div>
        <p data-pk-social-message>
            Nút mạng xã hội đang ở chế độ demo: OAuth chưa được cấu hình nên hệ thống
            chưa kết nối tới nhà cung cấp và không đăng nhập tài khoản nào.
        </p>
        <button type="button" class="btn-primary btn-block" data-pk-modal-close>Đã hiểu</button>
    </div>
</div>

<script src="${pageContext.request.contextPath}/js/auth.js"></script>
</body>
</html>
