<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Kết quả đặt cọc ZaloPay - PrintKraft</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css?v=20261104-1">
</head>
<body>
<%@ include file="header.jsp" %>
<c:set var="resolvedBackUrl" value="${empty backUrl ? '/quotes' : backUrl}"/>
<c:set var="resolvedBackLabel" value="${empty backLabel ? 'Quay lại Yêu cầu báo giá' : backLabel}"/>
<main class="shop-page payment-result-page">
    <section class="payment-result-card payment-result-${resultStatus}">
        <c:choose>
            <c:when test="${resultStatus eq 'success'}">
                <div class="payment-result-icon" aria-hidden="true">✓</div>
                <h1>Đặt cọc ZaloPay thành công</h1>
            </c:when>
            <c:when test="${resultStatus eq 'pending'}">
                <div class="payment-result-icon" aria-hidden="true">…</div>
                <h1>Đang xác nhận giao dịch ZaloPay</h1>
            </c:when>
            <c:when test="${resultStatus eq 'failed'}">
                <div class="payment-result-icon" aria-hidden="true">×</div>
                <h1>Thanh toán đặt cọc chưa thành công</h1>
            </c:when>
            <c:otherwise>
                <div class="payment-result-icon" aria-hidden="true">!</div>
                <h1>Không thể xác minh giao dịch ZaloPay</h1>
            </c:otherwise>
        </c:choose>
        <p class="payment-result-message"><c:out value="${resultMessage}"/></p>

        <c:if test="${not empty quoteRequestId or not empty requestCode or not empty appTransId}">
            <dl class="payment-result-details">
                <c:if test="${not empty quoteRequestId}">
                    <div><dt>Mã yêu cầu báo giá</dt><dd>#<c:out value="${quoteRequestId}"/></dd></div>
                </c:if>
                <c:if test="${not empty requestCode}">
                    <div><dt>Mã yêu cầu in 3D</dt><dd><c:out value="${requestCode}"/></dd></div>
                </c:if>
                <c:if test="${not empty productName}">
                    <div><dt>Sản phẩm / Mô hình</dt><dd><c:out value="${productName}"/></dd></div>
                </c:if>
                <c:if test="${not empty quotedAmount}">
                    <div><dt>Tổng giá xưởng báo</dt><dd><fmt:formatNumber value="${quotedAmount}" type="number" groupingUsed="true"/>đ</dd></div>
                </c:if>
                <c:if test="${not empty amount}">
                    <div><dt>Số tiền đặt cọc (30%)</dt><dd><fmt:formatNumber value="${amount}" type="number" groupingUsed="true"/>đ</dd></div>
                </c:if>
                <c:if test="${not empty appTransId}">
                    <div><dt>Mã giao dịch Merchant</dt><dd><c:out value="${appTransId}"/></dd></div>
                </c:if>
                <c:if test="${not empty zpTransId}">
                    <div><dt>Mã giao dịch ZaloPay</dt><dd><c:out value="${zpTransId}"/></dd></div>
                </c:if>
            </dl>
        </c:if>

        <div class="button-row payment-result-actions">
            <a class="button-primary" href="${pageContext.request.contextPath}${resolvedBackUrl}"><c:out value="${resolvedBackLabel}"/></a>
            <a class="button-secondary" href="${pageContext.request.contextPath}/home">Về trang chủ</a>
        </div>
        <c:if test="${resultStatus eq 'pending'}">
            <p class="muted payment-result-note">Nếu trạng thái chưa chuyển sang Đã đặt cọc, vui lòng kiểm tra lại sau ít phút.</p>
        </c:if>
    </section>
</main>
<%@ include file="footer.jsp" %>
</body>
</html>
