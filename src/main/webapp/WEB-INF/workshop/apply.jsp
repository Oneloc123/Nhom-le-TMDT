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
    <c:if test="${param.submitted eq '1'}">
        <div class="notice">Đã nhận hồ sơ. Xưởng của bạn đang chờ Admin xác minh.</div>
    </c:if>
    <c:choose>
        <c:when test="${not empty existingShop}">
            <section class="notice">
                <h2>Trạng thái hồ sơ xưởng</h2>
                <p><strong><c:out value="${existingShop.name}"/></strong></p>
                <c:choose>
                    <c:when test="${existingShop.verificationStatus eq 'PENDING'}">
                        <p>Hồ sơ đang chờ Admin xác minh. Bạn chưa thể truy cập trang quản trị xưởng.</p>
                    </c:when>
                    <c:when test="${existingShop.verificationStatus eq 'REJECTED'}">
                        <p>Hồ sơ chưa được chấp thuận. Vui lòng liên hệ Admin để được hướng dẫn cập nhật và gửi lại hồ sơ.</p>
                    </c:when>
                    <c:when test="${existingShop.verificationStatus eq 'VERIFIED' and existingShop.status ne 'ACTIVE'}">
                        <p>Xưởng đã được xác minh nhưng hiện đang tạm ngưng. Vui lòng liên hệ Admin để kích hoạt lại.</p>
                    </c:when>
                    <c:otherwise>
                        <p>Hồ sơ xưởng hiện chưa đủ điều kiện truy cập. Vui lòng liên hệ Admin.</p>
                    </c:otherwise>
                </c:choose>
                <p class="muted">Trạng thái xác minh: <c:out value="${existingShop.verificationStatus}"/> · Trạng thái hoạt động: <c:out value="${existingShop.status}"/></p>
            </section>
        </c:when>
        <c:otherwise>
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
        </c:otherwise>
    </c:choose>
    <a class="back-link" href="${pageContext.request.contextPath}/home">Quay lại marketplace</a>
</main>
</body>
</html>
