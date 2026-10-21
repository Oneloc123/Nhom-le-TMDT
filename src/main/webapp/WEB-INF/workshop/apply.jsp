<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng ký xưởng in · PrintKraft</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/workshop-ui/workshop.css">
</head>
<body class="apply-page">
<main class="apply-card">
    <a class="brand" href="${pageContext.request.contextPath}/home"><span class="brand-mark">P</span> PrintKraft</a>
    <div class="eyebrow">WORKSHOP PARTNER</div>
    <h1>Đăng ký xưởng in</h1>
    <p class="muted">Thông tin đăng ký sẽ được Admin xác minh trước khi xưởng xuất hiện trên marketplace.</p>
    <c:if test="${not empty error}"><div class="notice error"><c:out value="${error}"/></div></c:if>
    <form method="post" action="${pageContext.request.contextPath}/workshop/apply" class="form-grid">
        <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
        <label>Tên xưởng <input name="shopName" maxlength="180" required></label>
        <label>Số điện thoại <input name="contactPhone" maxlength="25" inputmode="tel"></label>
        <label>Email liên hệ <input name="contactEmail" type="email" maxlength="190"></label>
        <label>Tỉnh / thành phố <input name="province" maxlength="100"></label>
        <label>Quận / huyện <input name="district" maxlength="100"></label>
        <label class="span-2">Giới thiệu năng lực ban đầu <textarea name="description" maxlength="4000" rows="4"></textarea></label>
        <button class="primary span-2" type="submit">Gửi đăng ký chờ xác minh</button>
    </form>
    <a class="back-link" href="${pageContext.request.contextPath}/home">Quay lại marketplace</a>
</main>
</body>
</html>
