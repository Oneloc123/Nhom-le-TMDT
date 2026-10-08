<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng nhập — PrintKraft 3D</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth.css">
</head>
<body class="auth-body">

<div class="auth-shell">
    <aside class="auth-hero">
        <a class="brand" href="${pageContext.request.contextPath}/"><span class="cube"></span> PrintKraft <b>3D</b></a>
        <h1>In 3D <span class="accent">theo yêu cầu.</span></h1>
        <p>Tải file <b>.stl</b> / <b>.obj</b>, chọn công nghệ in <b>FDM</b> hoặc <b>SLA</b>,
            vật liệu <b>PLA · ABS · Resin</b> — báo giá theo trọng lượng và thời gian in.</p>
        <ul class="hero-points">
            <li>⚡ Báo giá tự động theo gram &amp; giờ in</li>
            <li>🖨️ 3 máy FDM · 2 máy SLA luôn sẵn sàng</li>
            <li>🚚 Giao bản in toàn quốc trong 48–72 giờ</li>
        </ul>
    </aside>

    <main class="auth-card">
        <h2>Đăng nhập</h2>
        <p class="sub">Chào mừng bạn quay lại xưởng in 3D PrintKraft.</p>

        <c:if test="${not empty registeredOk}">
            <div class="alert alert-ok"><c:out value="${registeredOk}"/></div>
        </c:if>
        <c:if test="${not empty resetOk}">
            <div class="alert alert-ok"><c:out value="${resetOk}"/></div>
        </c:if>
        <c:if test="${not empty error}">
            <div class="alert alert-err"><c:out value="${error}"/></div>
        </c:if>

        <form method="post" action="${pageContext.request.contextPath}/login" class="stack-form">
            <label>Email
                <input type="email" name="email" placeholder="vidu@gmail.com" required>
            </label>
            <label>Mật khẩu
                <input type="password" name="password" placeholder="••••••" required>
            </label>
            <div class="form-row">
                <label class="check"><input type="checkbox" name="remember"> Ghi nhớ đăng nhập</label>
                <a class="link" href="${pageContext.request.contextPath}/forgot-password">Quên mật khẩu?</a>
            </div>
            <button type="submit" class="btn-primary btn-block">Đăng nhập</button>
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

</body>
</html>
