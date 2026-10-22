<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng ký — PrintKraft 3D</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
    <script src="${pageContext.request.contextPath}/js/account.js" defer></script>
</head>
<body class="auth-body">

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
        <c:choose>

            <%-- Bước 2: xác thực email --%>
            <c:when test="${step == 'verify'}">
                <h2>Xác thực email</h2>
                <p class="sub">Chúng tôi đã gửi mã xác thực 6 số đến
                    <b><c:out value="${email}"/></b>.</p>

                <c:if test="${not empty error}">
                    <div class="alert alert-err"><c:out value="${error}"/></div>
                </c:if>

                <form method="post" action="${pageContext.request.contextPath}/register" class="stack-form">
                    <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                    <input type="hidden" name="action" value="verify">
                    <label>Mã xác thực
                        <input type="text" name="code" inputmode="numeric" pattern="[0-9]{6}"
                               maxlength="6" placeholder="6 chữ số" required autofocus>
                    </label>
                    <button type="submit" class="btn-primary btn-block">Kích hoạt tài khoản</button>
                </form>

                <p class="switch">Không nhận được email?
                    <a class="link" href="${pageContext.request.contextPath}/register">Gửi lại mã</a>
                </p>
            </c:when>

            <%-- Bước 1: form đăng ký --%>
            <c:otherwise>
                <h2>Đăng ký</h2>
                <p class="sub">Tài khoản khách hàng đặt in 3D PrintKraft.</p>

                <c:if test="${not empty error}">
                    <div class="alert alert-err"><c:out value="${error}"/></div>
                </c:if>

                <form method="post" action="${pageContext.request.contextPath}/register" class="stack-form" data-password-form>
                    <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                    <label>Họ và tên
                        <input type="text" name="fullName" placeholder="Nguyễn Văn A" required>
                    </label>
                    <label>Email
                        <input type="email" name="email" placeholder="vidu@gmail.com" required>
                    </label>
                    <div class="form-row-2">
                        <label>Số điện thoại
                            <input type="text" name="phone" placeholder="09xx xxx xxx">
                        </label>
                        <label for="registerPassword">Mật khẩu
                            <span class="password-control"><input id="registerPassword" type="password" name="password" minlength="8" autocomplete="new-password" placeholder="Ít nhất 8 ký tự" required data-password-strength><button type="button" class="password-toggle" data-password-toggle="registerPassword" aria-label="Hiện mật khẩu">Hiện</button></span>
                        </label>
                    </div>
                    <label for="registerConfirm">Xác nhận mật khẩu
                        <span class="password-control"><input id="registerConfirm" type="password" name="confirm" minlength="8" autocomplete="new-password" placeholder="Nhập lại mật khẩu" required data-password-confirm><button type="button" class="password-toggle" data-password-toggle="registerConfirm" aria-label="Hiện xác nhận mật khẩu">Hiện</button></span>
                    </label>
                    <div class="password-strength" aria-live="polite"><div class="strength-track"><span data-strength-bar></span></div><span data-strength-label>Hãy sử dụng mật khẩu dài và khó đoán.</span></div>
                    <label>Địa chỉ nhận bản in
                        <input type="text" name="address" placeholder="12 Nguyễn Huệ, Q.1, TP.HCM">
                    </label>
                    <button type="submit" class="btn-primary btn-block">Tạo tài khoản</button>
                </form>

                <div class="divider"><span>hoặc</span></div>

                <div class="social-row">
                    <a class="btn-social btn-google" href="${pageContext.request.contextPath}/login/google">
                        <span class="g">G</span> Đăng nhập bằng Google
                    </a>
                    <a class="btn-social btn-facebook" href="${pageContext.request.contextPath}/login/facebook">
                        <span class="f">f</span> Đăng nhập bằng Facebook
                    </a>
                </div>

                <p class="switch">Đã có tài khoản?
                    <a class="link" href="${pageContext.request.contextPath}/login">Đăng nhập</a>
                </p>
            </c:otherwise>
        </c:choose>
    </main>
</div>

</body>
</html>
