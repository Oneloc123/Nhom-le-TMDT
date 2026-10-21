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
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
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
        <div class="notice-ok">Đơn hàng của bạn đã được xác nhận.</div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="notice-err" role="alert"><c:out value="${error}"/></div>
    </c:if>

    <%-- Tìm kiếm theo mã đơn + lọc trạng thái (server-side, phân trang) --%>
    <form class="order-filter" method="get"
          action="${pageContext.request.contextPath}/purchase-history">
        <label>
            <span class="muted">Mã đơn hàng</span>
            <input type="search" name="q" value="<c:out value='${q}'/>"
                   placeholder="VD: PK-2025-0001" maxlength="40">
        </label>
        <label>
            <span class="muted">Trạng thái</span>
            <select name="status">
                <option value="">Tất cả trạng thái</option>
                <c:forEach var="s" items="${statuses}">
                    <option value="<c:out value='${s}'/>" ${status == s ? 'selected' : ''}>
                        <c:out value="${s}"/>
                    </option>
                </c:forEach>
            </select>
        </label>
        <button class="button-primary" type="submit">Lọc đơn hàng</button>
        <a class="button-secondary" href="${pageContext.request.contextPath}/purchase-history">Xóa bộ lọc</a>
    </form>
    <c:if test="${not empty total}">
        <p class="muted">Tìm thấy <b><c:out value="${total}"/></b> đơn hàng.</p>
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
                            <span class="status ${order.paidOrCod ? 'paid' : ''}">
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
                                <strong class="cart-total"><fmt:formatNumber value="${order.totalAmount}" type="number" groupingUsed="true"/>đ</strong>
                                <a class="button-secondary"
                                   href="${pageContext.request.contextPath}/order-detail?code=${order.orderCode}">Chi tiết</a>
                                <c:if test="${order.paymentStatus == 'Chưa thanh toán'}">
                                    <a class="button-primary" href="${pageContext.request.contextPath}/payment?orderNumber=${order.orderCode}">Thanh toán</a>
                                </c:if>
                            </div>
                        </div>
                    </article>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>

    <%-- Phân trang phía server --%>
    <c:if test="${not empty pages && pages > 1}">
        <nav class="sd-pagination" aria-label="Phân trang đơn hàng">
            <c:choose>
                <c:when test="${page > 1}">
                    <a href="${pageContext.request.contextPath}/purchase-history?page=${page - 1}&q=<c:out value='${q}'/>&status=<c:out value='${status}'/>">&laquo; Trước</a>
                </c:when>
                <c:otherwise><span class="disabled">&laquo; Trước</span></c:otherwise>
            </c:choose>
            <span class="current">Trang <c:out value="${page}"/> / <c:out value="${pages}"/></span>
            <c:choose>
                <c:when test="${page < pages}">
                    <a href="${pageContext.request.contextPath}/purchase-history?page=${page + 1}&q=<c:out value='${q}'/>&status=<c:out value='${status}'/>">Sau &raquo;</a>
                </c:when>
                <c:otherwise><span class="disabled">Sau &raquo;</span></c:otherwise>
            </c:choose>
        </nav>
    </c:if>
</main>
<%@ include file="footer.jsp" %>
</body>
</html>
