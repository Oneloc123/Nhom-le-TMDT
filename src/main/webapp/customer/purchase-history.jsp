<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Lịch sử mua hàng - PrintKraft</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css">
</head>
<body>
<%@ include file="header.jsp" %>

<main class="shop-page">
    <div class="shop-heading">
        <div>
            <div class="eyebrow-line">Đơn hàng</div>
            <h1>Lịch sử mua hàng</h1>
            <p>Theo dõi các đơn hàng bạn đã đặt và trạng thái thanh toán.</p>
        </div>
        <a class="button-secondary" href="${pageContext.request.contextPath}/products">Mua thêm sản phẩm</a>
    </div>

    <c:if test="${paid}">
        <div class="notice-ok">Thanh toán thành công. Đơn hàng của bạn đã được xác nhận.</div>
    </c:if>

    <c:choose>
        <c:when test="${empty orders}">
            <div class="empty-state">Bạn chưa có đơn hàng nào.</div>
        </c:when>
        <c:otherwise>
            <div class="history-list">
                <c:forEach var="order" items="${orders}">
                    <article class="history-card">
                        <div class="history-top">
                            <div>
                                <h2><c:out value="${order.orderNumber}"/></h2>
                                <span class="muted"><c:out value="${order.createdAt}"/> · <c:out value="${order.itemCount}"/> sản phẩm</span>
                            </div>
                            <span class="status ${order.paymentStatus == 'Đã thanh toán' ? 'paid' : ''}">
                                <c:out value="${order.paymentStatus}"/>
                            </span>
                        </div>
                        <div class="history-items">
                            <c:forEach var="item" items="${order.items}">
                                <div class="summary-line">
                                    <span><c:out value="${item.productName}"/> × <c:out value="${item.quantity}"/></span>
                                    <strong><fmt:formatNumber value="${item.subtotal}" type="number" groupingUsed="true"/>đ</strong>
                                </div>
                            </c:forEach>
                        </div>
                        <div class="history-bottom">
                            <div><span class="muted">Trạng thái đơn:</span> <b><c:out value="${order.status}"/></b></div>
                            <div class="button-row">
                                <strong class="cart-total"><fmt:formatNumber value="${order.total}" type="number" groupingUsed="true"/>đ</strong>
                                <c:if test="${order.paymentStatus != 'Đã thanh toán'}">
                                    <a class="button-primary" href="${pageContext.request.contextPath}/payment?orderNumber=${order.orderNumber}">Thanh toán</a>
                                </c:if>
                            </div>
                        </div>
                    </article>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</main>
<%@ include file="footer.jsp" %>
</body>
</html>
