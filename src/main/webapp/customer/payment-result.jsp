<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Kết quả thanh toán VNPAY - PrintKraft</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css?v=20261104-1">
</head>
<body>
<%@ include file="header.jsp" %>
<c:url var="retryPaymentUrl" value="/payment">
    <c:param name="orderNumber" value="${orderCode}"/>
</c:url>
<main class="shop-page payment-result-page">
    <section class="payment-result-card payment-result-${resultStatus}">
        <c:choose>
            <c:when test="${resultStatus eq 'success'}"><div class="payment-result-icon" aria-hidden="true">✓</div><h1>Thanh toán thành công</h1></c:when>
            <c:when test="${resultStatus eq 'pending'}"><div class="payment-result-icon" aria-hidden="true">…</div><h1>Đang xác nhận giao dịch</h1></c:when>
            <c:when test="${resultStatus eq 'failed'}"><div class="payment-result-icon" aria-hidden="true">×</div><h1>Thanh toán chưa thành công</h1></c:when>
            <c:otherwise><div class="payment-result-icon" aria-hidden="true">!</div><h1>Không thể xác minh giao dịch</h1></c:otherwise>
        </c:choose>
        <p class="payment-result-message"><c:out value="${resultMessage}"/></p>

        <c:if test="${not empty orderCode}">
            <dl class="payment-result-details">
                <div><dt>Mã đơn hàng</dt><dd><c:out value="${orderCode}"/></dd></div>
                <c:if test="${not empty amount}"><div><dt>Số tiền</dt><dd><fmt:formatNumber value="${amount}" type="number" groupingUsed="true"/>đ</dd></div></c:if>
                <c:if test="${not empty transactionNo}"><div><dt>Mã giao dịch VNPAY</dt><dd><c:out value="${transactionNo}"/></dd></div></c:if>
                <c:if test="${not empty bankCode}"><div><dt>Ngân hàng</dt><dd><c:out value="${bankCode}"/></dd></div></c:if>
            </dl>
        </c:if>

        <div class="button-row payment-result-actions">
            <a class="button-primary" href="${pageContext.request.contextPath}/purchase-history">Xem lịch sử mua hàng</a>
            <c:if test="${resultStatus eq 'failed' and not empty orderCode}">
                <a class="button-secondary" href="${retryPaymentUrl}">Thử thanh toán lại</a>
            </c:if>
            <a class="button-secondary" href="${pageContext.request.contextPath}/home">Về trang chủ</a>
        </div>
        <c:if test="${resultStatus eq 'pending'}">
            <p class="muted payment-result-note">Nếu trạng thái chưa cập nhật, vui lòng chờ ít phút rồi mở lịch sử mua hàng.</p>
        </c:if>
    </section>
</main>
<%@ include file="footer.jsp" %>
</body>
</html>
