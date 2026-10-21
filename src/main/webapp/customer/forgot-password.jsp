<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quên mật khẩu — PrintKraft 3D</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/bootstrap.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth.css">
</head>
<body class="auth-body" data-pk-ctx="${pageContext.request.contextPath}">

<div class="auth-shell">
    <aside class="auth-hero">
        <a class="brand" href="${pageContext.request.contextPath}/"><span class="cube"></span> PrintKraft <b>3D</b></a>
        <h1>Khôi phục <span class="accent">mật khẩu.</span></h1>
        <p>Nhập email đã đăng ký, hệ thống phát hành OTP và bạn đặt lại mật khẩu
            để tiếp tục theo dõi các đơn in 3D của mình.</p>
        <ul class="hero-points">
            <li>OTP 6 chữ số, hiệu lực 5 phút</li>
            <li>Chế độ demo: mã kiểm thử là 111111</li>
        </ul>
    </aside>

    <main class="auth-card">
        <c:if test="${not empty error}">
            <div class="alert alert-err" role="alert"><c:out value="${error}"/></div>
        </c:if>

        <%-- ============ Bước 1: nhập email (phản hồi chung, không lộ tài khoản) ============ --%>
        <div data-pk-step="email" <c:if test="${step == 'otp'}">hidden</c:if>>
            <h2>Quên mật khẩu?</h2>
            <p class="sub">Nhập email đã đăng ký để nhận mã OTP khôi phục.</p>

            <form method="post" action="${pageContext.request.contextPath}/forgot-password"
                  class="stack-form" data-pk-form="forgot">
                <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                <input type="hidden" name="action" value="send">
                <label>Email
                    <input type="email" name="email" placeholder="vidu@gmail.com"
                           maxlength="190" autocomplete="email" required autofocus>
                </label>
                <button type="submit" class="btn-primary btn-block">Gửi mã OTP (demo)</button>
            </form>

            <p class="switch">Nhớ mật khẩu rồi?
                <a class="link" href="${pageContext.request.contextPath}/login">Đăng nhập</a>
            </p>
        </div>

        <%-- ============ Bước 2: OTP + mật khẩu mới ============ --%>
        <div data-pk-step="reset" <c:if test="${step != 'otp'}">hidden</c:if>>
            <div class="otp-head">
                <h2>Đặt lại mật khẩu</h2>
                <div class="demo-badge" title="Chế độ demo">DEMO</div>
            </div>
            <p class="sub">Yêu cầu khôi phục cho <b data-pk-reset-email><c:out value="${email}"/></b>
                có hiệu lực trong 5 phút.</p>

            <div class="demo-note" data-pk-reset-note>
                <b>Chế độ demo:</b> mã OTP kiểm thử là 111111; hệ thống chưa gửi email thật.
            </div>
            <div class="demo-code-row">
                <span>Mã OTP kiểm thử:</span>
                <code class="demo-code" data-pk-reset-code>111111</code>
            </div>

            <form method="post" action="${pageContext.request.contextPath}/forgot-password"
                  class="stack-form" data-pk-form="forgot-reset">
                <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                <input type="hidden" name="action" value="reset">
                <input type="hidden" name="email" value="<c:out value='${email}'/>"
                       data-pk-reset-bound autocomplete="off">
                <label>Mã OTP
                    <input type="text" name="otp" inputmode="numeric" pattern="[0-9]{6}"
                           maxlength="6" placeholder="111111" autocomplete="one-time-code" required autofocus>
                </label>
                <label>Mật khẩu mới
                    <span class="pass-field">
                        <input type="password" name="password" id="resetPassword"
                               placeholder="Tối thiểu 6 ký tự" autocomplete="new-password" required>
                        <button type="button" class="pass-toggle" data-pk-toggle-pass="resetPassword"
                                aria-pressed="false" aria-label="Hiện/ẩn mật khẩu">Hiện</button>
                    </span>
                </label>
                <label>Xác nhận mật khẩu mới
                    <span class="pass-field">
                        <input type="password" name="confirm" id="resetConfirm"
                               autocomplete="new-password" required>
                        <button type="button" class="pass-toggle" data-pk-toggle-pass="resetConfirm"
                                aria-pressed="false" aria-label="Hiện/ẩn mật khẩu">Hiện</button>
                    </span>
                </label>
                <button type="submit" class="btn-primary btn-block">Đặt lại mật khẩu</button>
            </form>

            <p class="switch">
                <a class="link" href="${pageContext.request.contextPath}/login">Quay lại đăng nhập</a>
                &nbsp;·&nbsp;
                <a class="link" href="${pageContext.request.contextPath}/forgot-password">Yêu cầu mã mới</a>
            </p>
        </div>
    </main>
</div>

<script src="${pageContext.request.contextPath}/js/auth.js"></script>
</body>
</html>
