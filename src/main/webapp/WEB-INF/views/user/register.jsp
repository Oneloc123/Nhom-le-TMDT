<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Tạo tài khoản | PrintKraft</title><link rel="stylesheet" href="${pageContext.request.contextPath}/css/user-v2.css"></head>
<body class="uv2-auth-body"><main class="uv2-auth-wrap"><section class="uv2-auth-brand"><a class="uv2-brand" href="${pageContext.request.contextPath}/home">PrintKraft <span>3D</span></a><div class="uv2-brand-art" aria-hidden="true">◇</div><p class="uv2-eyebrow">BẮT ĐẦU HÀNH TRÌNH</p><h1>Tài khoản của<br><span>riêng bạn.</span></h1><p>Lưu thông tin giao hàng, xem lịch sử mua sắm và quản lý bản in theo yêu cầu dễ dàng hơn.</p></section>
<section class="uv2-auth-card"><a class="uv2-back" href="${pageContext.request.contextPath}/login">← Đã có tài khoản</a>
<c:choose>
<c:when test="${step eq 'verify'}">
<h2>Xác thực email</h2><p class="uv2-muted">Nhập mã 6 chữ số đã gửi đến <strong><c:out value="${email}"/></strong>. Mã có hiệu lực trong 15 phút.</p>
<c:if test="${not empty error}"><div class="uv2-alert is-error" role="alert"><c:out value="${error}"/></div></c:if>
<c:if test="${not empty success}"><div class="uv2-alert is-success" role="status"><c:out value="${success}"/></div></c:if>
<form method="post" action="${pageContext.request.contextPath}/register" class="uv2-form">
<input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"><input type="hidden" name="action" value="verify"><input type="hidden" name="email" value="<c:out value='${email}'/>">
<label>Mã xác thực<input name="code" inputmode="numeric" pattern="[0-9]{6}" minlength="6" maxlength="6" required autocomplete="one-time-code" placeholder="••••••"></label>
<button class="uv2-button is-primary" type="submit">Xác nhận email</button></form>
<form method="post" action="${pageContext.request.contextPath}/register" class="uv2-resend-form"><input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"><input type="hidden" name="action" value="resend"><input type="hidden" name="email" value="<c:out value='${email}'/>"><button class="uv2-link-button" type="submit">Gửi lại mã xác thực</button></form>
<p class="uv2-small">Sau khi kích hoạt, bạn có thể đăng nhập và sử dụng tài khoản.</p>
</c:when>
<c:otherwise>
<h2>Tạo tài khoản</h2><p class="uv2-muted">Chỉ mất vài thông tin cơ bản để bắt đầu.</p>
<c:if test="${not empty error}"><div class="uv2-alert is-error" role="alert"><c:out value="${error}"/></div></c:if>
<form method="post" action="${pageContext.request.contextPath}/register" class="uv2-form">
<input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
<label>Họ và tên<input name="fullName" required minlength="2" maxlength="120" autocomplete="name" value="<c:out value='${fullName}'/>" placeholder="Nguyễn Văn A"></label>
<label>Email<input type="email" name="email" required maxlength="190" autocomplete="email" value="<c:out value='${email}'/>" placeholder="ban@example.com"></label>
<label>Số điện thoại <span class="uv2-small">(không bắt buộc)</span><input name="phone" maxlength="25" autocomplete="tel" value="<c:out value='${phone}'/>" placeholder="09xx xxx xxx"></label>
<label>Mật khẩu<input type="password" name="password" required minlength="8" maxlength="72" autocomplete="new-password" placeholder="Ít nhất 8 ký tự"></label>
<label>Nhập lại mật khẩu<input type="password" name="confirmPassword" required minlength="8" maxlength="72" autocomplete="new-password"></label>
<button class="uv2-button is-primary" type="submit">Tạo tài khoản</button></form>
<p class="uv2-small">Bạn cần xác thực email để kích hoạt tài khoản. Khi tạo tài khoản, bạn đồng ý sử dụng dịch vụ theo các điều khoản của PrintKraft.</p>
</c:otherwise>
</c:choose>
</section></main></body></html>
