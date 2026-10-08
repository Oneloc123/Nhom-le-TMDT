<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quên mật khẩu — PrintKraft 3D</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth.css">
</head>
<body class="auth-body">

<div class="auth-shell">
    <aside class="auth-hero">
        <a class="brand" href="${pageContext.request.contextPath}/"><span class="cube"></span> PrintKraft <b>3D</b></a>
        <h1>Khôi phục <span class="accent">mật khẩu.</span></h1>
        <p>Chúng tôi sẽ gửi OTP đến email đã đăng ký để bạn đặt lại mật khẩu
            và tiếp tục theo dõi các đơn in 3D của mình.</p>
        <ul class="hero-points">
            <li>🔐 OTP 6 chữ số, có giới hạn thời gian</li>
            <li>✉️ Gửi tới email bạn dùng đăng ký tài khoản</li>
        </ul>
    </aside>

    <main class="auth-card">
        <c:choose>

            <%-- Bước 2: nhập OTP + mật khẩu mới --%>
            <c:when test="${step == 'otp'}">
                <h2>Đặt lại mật khẩu</h2>
                <p class="sub">OTP đã được gửi tới <b><c:out value="${email}"/></b>.</p>

                <c:if test="${not empty error}">
                    <div class="alert alert-err"><c:out value="${error}"/></div>
                </c:if>

                <form method="post" action="${pageContext.request.contextPath}/forgot-password" class="stack-form">
                    <input type="hidden" name="action" value="reset">
                    <label>Mã OTP
                        <input type="text" name="otp" inputmode="numeric" pattern="[0-9]{6}"
                               maxlength="6" placeholder="6 chữ số" required autofocus>
                    </label>
                    <label>Mật khẩu mới
                        <input type="password" name="password" placeholder="Tối thiểu 6 ký tự" required>
                    </label>
                    <label>Xác nhận mật khẩu mới
                        <input type="password" name="confirm" placeholder="Nhập lại mật khẩu" required>
                    </label>
                    <button type="submit" class="btn-primary btn-block">Đặt lại mật khẩu</button>
                </form>

                <p class="switch">
                    <a class="link" href="${pageContext.request.contextPath}/login">Quay lại đăng nhập</a>
                </p>
            </c:when>

            <%-- Bước 1: nhập email --%>
            <c:otherwise>
                <h2>Quên mật khẩu?</h2>
                <p class="sub">Nhập email đã đăng ký, chúng tôi sẽ gửi mã OTP khôi phục.</p>

                <c:if test="${not empty error}">
                    <div class="alert alert-err"><c:out value="${error}"/></div>
                </c:if>

                <form method="post" action="${pageContext.request.contextPath}/forgot-password" class="stack-form">
                    <input type="hidden" name="action" value="send">
                    <label>Email
                        <input type="email" name="email" placeholder="vidu@gmail.com" required autofocus>
                    </label>
                    <button type="submit" class="btn-primary btn-block">Gửi mã OTP</button>
                </form>

                <p class="switch">Nhớ mật khẩu rồi?
                    <a class="link" href="${pageContext.request.contextPath}/login">Đăng nhập</a>
                </p>
            </c:otherwise>
        </c:choose>
    </main>
</div>

</body>
</html>
