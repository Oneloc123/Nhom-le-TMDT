<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng nhập — PrintKraft 3D</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/bootstrap.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth.css">
</head>
<body class="auth-body" data-pk-ctx="${pageContext.request.contextPath}">

<div class="auth-shell">
    <aside class="auth-hero">
        <a class="brand" href="${pageContext.request.contextPath}/"><span class="cube"></span> PrintKraft <b>3D</b></a>
        <h1>In 3D <span class="accent">theo yêu cầu.</span></h1>
        <p>Tải file <b>.stl</b> / <b>.obj</b>, chọn công nghệ in <b>FDM</b> hoặc <b>SLA</b>,
            vật liệu <b>PLA · ABS · Resin</b> — báo giá theo trọng lượng và thời gian in.</p>
        <ul class="hero-points">
            <li>Báo giá tự động theo gram &amp; giờ in</li>
            <li>3 máy FDM · 2 máy SLA luôn sẵn sàng</li>
            <li>Giao bản in toàn quốc trong 48–72 giờ</li>
        </ul>
    </aside>

    <main class="auth-card">
        <h2>Đăng nhập</h2>
        <p class="sub">Chào mừng bạn quay lại xưởng in 3D PrintKraft.</p>

        <c:if test="${not empty registeredOk}">
            <div class="alert alert-ok" role="status"><c:out value="${registeredOk}"/></div>
        </c:if>
        <c:if test="${not empty resetOk}">
            <div class="alert alert-ok" role="status"><c:out value="${resetOk}"/></div>
        </c:if>
        <c:if test="${not empty socialDemo}">
            <div class="alert alert-err" role="alert"><c:out value="${socialDemo}"/></div>
        </c:if>
        <c:if test="${not empty error}">
            <div class="alert alert-err" role="alert"><c:out value="${error}"/></div>
        </c:if>

        <%-- ============ Mật khẩu (mặc định) ============ --%>
        <div data-pk-panel="login-password" <c:if test="${step == 'otp'}">hidden</c:if>>
            <form method="post" action="${pageContext.request.contextPath}/login"
                  class="stack-form" data-pk-form="login" autocomplete="on">
                <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                <label>Email
                    <input type="email" name="email" placeholder="vidu@gmail.com"
                           autocomplete="username" required>
                </label>
                <label>Mật khẩu
                    <span class="pass-field">
                        <input type="password" name="password" id="loginPassword"
                               placeholder="••••••" autocomplete="current-password" required>
                        <button type="button" class="pass-toggle" data-pk-toggle-pass="loginPassword"
                                aria-pressed="false" aria-label="Hiện/ẩn mật khẩu">Hiện</button>
                    </span>
                </label>
                <div class="form-row">
                    <label class="check"><input type="checkbox" name="remember"> Ghi nhớ đăng nhập</label>
                    <a class="link" href="${pageContext.request.contextPath}/forgot-password">Quên mật khẩu?</a>
                </div>
                <button type="submit" class="btn-primary btn-block">Đăng nhập</button>
                <p class="switch" style="text-align:center;">
                    <a class="link" href="#" data-pk-show="otp">Đăng nhập bằng OTP &raquo;</a>
                </p>
            </form>
        </div>

        <%-- ============ OTP demo 111111 ============ --%>
        <div data-pk-panel="login-otp" <c:if test="${step != 'otp'}">hidden</c:if>>
            <form method="post" action="${pageContext.request.contextPath}/login"
                  class="stack-form" data-pk-form="login-otp">
                <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                <div class="otp-head">
                    <h3>Đăng nhập bằng OTP</h3>
                    <div class="demo-badge" title="Chế độ demo">DEMO</div>
                </div>
                <div class="demo-note" data-pk-otp-demo-note <c:if test="${step != 'otp'}">hidden</c:if>>
                    <b>Chế độ demo:</b> mã OTP kiểm thử là 111111; hệ thống chưa gửi email thật.
                </div>
                <label>Email
                    <input type="email" name="email" value="<c:out value='${email}'/>"
                           placeholder="vidu@gmail.com" autocomplete="username" required>
                </label>
                <button type="submit" name="action" value="otp_request"
                        class="btn-secondary btn-block" data-pk-otp-request>Gửi mã OTP (demo)</button>

                <label data-pk-otp-code-row <c:if test="${step != 'otp'}">hidden</c:if>>
                    Mã OTP
                    <input type="text" name="code" inputmode="numeric" pattern="[0-9]{6}"
                           maxlength="6" placeholder="111111" autocomplete="one-time-code">
                </label>
                <button type="submit" name="action" value="otp_verify"
                        class="btn-primary btn-block" data-pk-otp-verify
                        <c:if test="${step != 'otp'}">hidden</c:if>>Xác thực và đăng nhập</button>

                <p class="switch" style="text-align:center;">
                    <a class="link" href="#" data-pk-show="password">&laquo; Quay lại đăng nhập mật khẩu</a>
                </p>
            </form>
        </div>

        <div class="divider"><span>hoặc</span></div>

        <div class="social-row">
            <button type="button" class="btn-social btn-google" data-pk-social="google"
                    aria-haspopup="dialog">
                <span class="g" aria-hidden="true">G</span> Đăng nhập bằng Google
            </button>
            <button type="button" class="btn-social btn-facebook" data-pk-social="facebook"
                    aria-haspopup="dialog">
                <span class="f" aria-hidden="true">f</span> Đăng nhập bằng Facebook
            </button>
        </div>
        <p class="social-demo-hint">Nút mạng xã hội đang ở chế độ demo — chưa kết nối OAuth thật.</p>

        <p class="switch">Chưa có tài khoản?
            <a class="link" href="${pageContext.request.contextPath}/register">Đăng ký ngay</a>
        </p>

        <div class="demo-note">
            <b>Tài khoản demo</b>
            Admin: admin@printkraft.vn / admin123 &nbsp;·&nbsp;
            Khách: minhtuan@gmail.com / 123456
        </div>
    </main>
</div>

<%-- Modal thông báo OAuth chưa cấu hình (chỉ demo giao diện, không gọi nhà cung cấp) --%>
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
        <p class="pk-modal-foot">
            Khi triển khai thật, chủ dự án cấp client ID/secret và bật luồng OAuth
            (xem chú thích <code>TODO(OAUTH)</code> trong <code>GoogleLoginServlet</code> /
            <code>FacebookLoginServlet</code>).
        </p>
        <button type="button" class="btn-primary btn-block" data-pk-modal-close>Đã hiểu</button>
    </div>
</div>

<script src="${pageContext.request.contextPath}/js/auth.js"></script>
</body>
</html>
