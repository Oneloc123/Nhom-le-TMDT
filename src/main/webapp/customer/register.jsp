<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng ký — PrintKraft 3D</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth.css">
</head>
<body class="auth-body">

<div class="auth-shell">
    <aside class="auth-hero">
        <a class="brand" href="${pageContext.request.contextPath}/"><span class="cube"></span> PrintKraft <b>3D</b></a>
        <h1>Tạo tài khoản, <span class="accent">bắt đầu in.</span></h1>
        <p>Lưu địa chỉ nhận bản in, theo dõi đơn <b>.stl</b> / <b>.obj</b> đang in theo thời gian thực
            và nhận báo giá FDM / SLA theo trọng lượng vật liệu.</p>
        <ul class="hero-points">
            <li>🧾 Báo giá PLA · ABS · Resin theo gram</li>
            <li>⏱️ Ước tính thời gian in trước khi đặt</li>
            <li>📦 Theo dõi trạng thái: Mới → Đang in → Hoàn tất → Đã giao</li>
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

                <form method="post" action="${pageContext.request.contextPath}/register" class="stack-form">
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
                        <label>Mật khẩu
                            <input type="password" name="password" placeholder="Tối thiểu 6 ký tự" required>
                        </label>
                    </div>
                    <label>Xác nhận mật khẩu
                        <input type="password" name="confirm" placeholder="Nhập lại mật khẩu" required>
                    </label>
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
