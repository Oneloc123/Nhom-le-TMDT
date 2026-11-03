<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Theo dõi đơn hàng - PrintKraft</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css">
</head>
<body>
<%@ include file="header.jsp" %>
<jsp:include page="sidebar.jsp" />
<main class="shop-page account-tracking-page" data-tracking-code="<c:out value='${order.orderCode}'/>" data-context-path="${pageContext.request.contextPath}">
    <div class="shop-heading"><div><div class="eyebrow-line">Theo dõi đơn</div><h1><c:out value="${order.orderCode}"/></h1><p>Cập nhật gần nhất từ đơn hàng, xưởng in và vận chuyển.</p></div><a class="button-secondary" href="${pageContext.request.contextPath}/order-detail?code=${order.orderCode}">Chi tiết đơn</a></div>
    <section class="account-tracking-summary" aria-live="polite">
        <div><span>Trạng thái đơn</span><strong id="trackingStatus"><c:out value="${order.status}"/></strong></div>
        <div><span>Thanh toán</span><strong id="trackingPayment"><c:out value="${order.paymentStatus}"/></strong></div>
        <div><span>Vận chuyển</span><strong id="trackingShipping"><c:out value="${order.shippingStatus}"/></strong></div>
    </section>
    <section class="account-panel account-timeline"><h2>Hành trình đơn hàng</h2>
        <c:choose><c:when test="${empty trackingEvents}"><p class="account-empty">Chưa có cập nhật tiến độ.</p></c:when><c:otherwise><ol id="trackingTimeline" class="tracking-timeline"><c:forEach var="event" items="${trackingEvents}"><li><div><strong><c:out value="${event.title}"/></strong><c:if test="${not empty event.details}"><p><c:out value="${event.details}"/></p></c:if><small><c:out value="${event.source}"/> · <c:out value="${event.when}"/></small></div></li></c:forEach></ol></c:otherwise></c:choose>
    </section>
    <section class="account-panel tracking-share-panel">
        <h2>Liên kết chia sẻ trạng thái</h2>
        <p>Người có liên kết chỉ xem mã đơn, trạng thái và mốc tiến độ cơ bản; không xem số điện thoại hoặc địa chỉ nhận hàng.</p>
        <c:if test="${param.shareUpdated == '1'}"><p class="account-inline-success" role="status">Đã cập nhật cài đặt chia sẻ.</p></c:if>
        <c:if test="${param.shareError == '1'}"><p class="account-inline-error" role="alert">Không cập nhật được cài đặt chia sẻ.</p></c:if>
        <form method="post" action="${pageContext.request.contextPath}/order-tracking" class="tracking-share-form">
            <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
            <input type="hidden" name="action" value="toggleShare"><input type="hidden" name="code" value="<c:out value='${order.orderCode}'/>">
            <label><input type="checkbox" name="enabled" value="true" <c:if test="${trackingShareEnabled}">checked</c:if>> Cho phép truy cập liên kết chia sẻ</label>
            <button class="account-outline-button" type="submit">Lưu cài đặt chia sẻ</button>
        </form>
        <c:if test="${trackingShareEnabled}"><div class="tracking-share-url"><input id="trackingShareUrl" type="text" readonly value="<c:out value='${trackingShareUrl}'/>" aria-label="Liên kết chia sẻ trạng thái đơn hàng"><button class="account-outline-button" type="button" data-copy-tracking>Copy liên kết</button></div></c:if>
    </section>
    <p class="account-muted-note" id="trackingRefreshStatus" aria-live="polite">Trang tự kiểm tra cập nhật khoảng 30 giây một lần khi còn mở.</p>
</main>
<%@ include file="footer.jsp" %>
<script src="${pageContext.request.contextPath}/js/order-tracking.js" defer></script>
</body>
</html>
