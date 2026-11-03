<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="vi"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Đăng nhập | PrintKraft</title><link rel="stylesheet" href="${pageContext.request.contextPath}/css/user-v2.css">
</head><body class="uv2-auth-body">
<main class="uv2-auth-wrap"><section class="uv2-auth-brand"><a class="uv2-brand" href="${pageContext.request.contextPath}/home">PrintKraft <span>3D</span></a><div class="uv2-brand-art" aria-hidden="true">◇</div><p class="uv2-eyebrow">TÀI KHOẢN PRINTKRAFT</p><h1>Sáng tạo mô hình.<br><span>Hiện thực hóa ý tưởng.</span></h1><p>Quản lý đơn hàng, lưu địa chỉ giao hàng và theo dõi các yêu cầu in 3D tại một nơi.</p></section>
<section class="uv2-auth-card"><a class="uv2-back" href="${pageContext.request.contextPath}/home">← Về trang chủ</a><h2>Chào mừng trở lại</h2><p class="uv2-muted">Đăng nhập để tiếp tục với PrintKraft.</p>
<c:if test="${not empty error}"><div class="uv2-alert is-error" role="alert"><c:out value="${error}"/></div></c:if>
<c:if test="${not empty resetOk}"><div class="uv2-alert is-success"><c:out value="${resetOk}"/></div></c:if>
<c:if test="${param.registered eq '1'}"><div class="uv2-alert is-success">Email đã được xác thực. Bạn có thể đăng nhập ngay.</div></c:if>
<c:if test="${param.loggedOut eq '1'}"><div class="uv2-alert is-success">Bạn đã đăng xuất thành công.</div></c:if>
<c:if test="${param.oauth eq 'unavailable'}"><div class="uv2-alert is-error">Đăng nhập Google/Facebook hiện chưa được cấu hình xác thực OAuth an toàn. Vui lòng đăng nhập bằng email và mật khẩu.</div></c:if>
<form method="post" action="${pageContext.request.contextPath}/login" class="uv2-form">
<input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
<label>Email<input type="email" name="email" maxlength="190" autocomplete="username" required value="<c:out value='${email}'/>" placeholder="ban@example.com"></label>
<label>Mật khẩu<input type="password" name="password" autocomplete="current-password" required maxlength="72" placeholder="Nhập mật khẩu"></label>
<c:if test="${not empty param.redirect}"><input type="hidden" name="redirect" value="<c:out value='${param.redirect}'/>"/></c:if>
<div class="uv2-form-between"><span class="uv2-small">Không chia sẻ mật khẩu với người khác.</span><a href="${pageContext.request.contextPath}/forgot-password">Quên mật khẩu?</a></div>
<button class="uv2-button is-primary" type="submit">Đăng nhập</button><p class="uv2-small uv2-dev-note">
                                                                          **TÀI KHOẢN DEMO:**
                                                                          Admin: admin@printkraft.local / Admin@12345 |
                                                                          Xưởng: workshop@printkraft.local / Workshop@12345 |
                                                                          Khách hàng: demo@printkraft.local / PrintKraft@123
                                                                      </p></form>
<p class="uv2-small uv2-dev-note">Đăng nhập mạng xã hội tạm thời chưa bật; chỉ sử dụng khi đã cấu hình OAuth2 thật.</p>
<p class="uv2-auth-switch">Chưa có tài khoản? <a href="${pageContext.request.contextPath}/register">Tạo tài khoản</a></p>
<p class="uv2-small uv2-dev-note">Tài khoản demo: demo@printkraft.local · Mật khẩu: PrintKraft@123</p>
</section></main></body></html>
