<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Chi tiết đơn hàng - PrintKraft</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css">
</head>
<body>
<%@ include file="header.jsp" %>

<main class="shop-page">
    <div class="shop-heading">
        <div>
            <div class="eyebrow-line">Đơn hàng</div>
            <h1>Chi tiết đơn <c:out value="${order.orderCode}"/></h1>
            <p>Đặt lúc <c:out value="${order.createdAt}"/> · <c:out value="${order.status}"/></p>
        </div>
        <a class="button-secondary" href="${pageContext.request.contextPath}/purchase-history">Quay lại lịch sử</a>
    </div>

    <c:if test="${cancelled}">
        <div class="notice-ok">Đơn hàng đã được hủy.</div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="notice-error"><c:out value="${error}"/></div>
    </c:if>

    <div class="checkout-layout">
        <section class="summary-panel">
            <h2>Sản phẩm trong đơn</h2>
            <div class="cart-list">
                <c:forEach var="item" items="${order.items}">
                    <div class="cart-line">
                        <div>
                            <h3><c:out value="${item.productName}"/></h3>
                            <p>Gian hàng: <c:out value="${item.shopName}"/> ·
                                <fmt:formatNumber value="${item.unitPrice}" type="number" groupingUsed="true"/>đ ×
                                <c:out value="${item.quantity}"/> · trạng thái: <c:out value="${item.status}"/></p>
                        </div>
                        <strong><fmt:formatNumber value="${item.lineTotal}" type="number" groupingUsed="true"/>đ</strong>
                        <c:choose>
                            <c:when test="${item.reviewed}">
                                <a class="review-action-btn reviewed"
                                   href="${pageContext.request.contextPath}/review?item=${item.orderItemId}">★ Đã đánh giá (Sửa)</a>
                            </c:when>
                            <c:when test="${item.reviewable}">
                                <a class="review-action-btn to-review"
                                   href="${pageContext.request.contextPath}/review?item=${item.orderItemId}">★ Đánh giá ngay</a>
                            </c:when>
                        </c:choose>
                    </div>
                </c:forEach>
            </div>

            <div class="summary-line" style="margin-top:18px;"><span>Tạm tính</span>
                <strong><fmt:formatNumber value="${order.subtotal}" type="number" groupingUsed="true"/>đ</strong></div>
            <div class="summary-line" style="margin-top:8px;"><span>Phí vận chuyển</span>
                <strong><fmt:formatNumber value="${order.shippingFee}" type="number" groupingUsed="true"/>đ</strong></div>
            <div class="summary-line" style="margin-top:8px;"><strong>Tổng cộng</strong>
                <strong class="cart-total"><fmt:formatNumber value="${order.totalAmount}" type="number" groupingUsed="true"/>đ</strong></div>

            <c:if test="${order.cancellable}">
                <form method="post" action="${pageContext.request.contextPath}/order-detail"
                      style="margin-top:20px;" onsubmit="return confirm('Bạn chắc chắn muốn hủy đơn này?');">
                    <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                    <input type="hidden" name="action" value="cancel">
                    <input type="hidden" name="code" value="<c:out value='${order.orderCode}'/>">
                    <button class="button-danger" type="submit">Hủy đơn hàng</button>
                </form>
            </c:if>
        </section>

        <aside class="order-panel">
            <h2>Nhận hàng</h2>
            <div class="summary-line"><span>Người nhận</span><b><c:out value="${order.recipientName}"/></b></div>
            <div class="summary-line" style="margin-top:10px;"><span>Số điện thoại</span><b><c:out value="${order.recipientPhone}"/></b></div>
            <div class="summary-line" style="margin-top:10px;"><span>Địa chỉ</span><b><c:out value="${order.shippingAddress}"/></b></div>
            <div class="summary-line" style="margin-top:10px;"><span>Thanh toán</span><b><c:out value="${order.paymentStatus}"/></b></div>
            <div class="summary-line" style="margin-top:10px;"><span>Vận chuyển</span><b><c:out value="${order.shippingStatus}"/></b></div>

            <h2 style="margin-top:26px;">Lịch sử trạng thái</h2>
            <c:choose>
                <c:when test="${empty order.timeline}">
                    <p class="muted">Chưa có cập nhật trạng thái.</p>
                </c:when>
                <c:otherwise>
                    <div class="cart-list">
                        <c:forEach var="h" items="${order.timeline}">
                            <div class="order-line">
                                <span><b><c:out value="${h.newStatus}"/></b>
                                    <span class="muted">— <c:out value="${h.note}"/></span></span>
                                <span class="muted"><c:out value="${h.changedAtText}"/></span>
                            </div>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>

            <c:if test="${not empty order.productionUpdates}">
                <h2 style="margin-top:26px;">Tiến độ sản xuất</h2>
                <div class="cart-list">
                    <c:forEach var="pu" items="${order.productionUpdates}">
                        <div class="order-line">
                            <span><b><c:out value="${pu.stage}"/></b> (<c:out value="${pu.progressPercent}"/>%) —
                                <c:out value="${pu.message}"/></span>
                            <span class="muted"><c:out value="${pu.createdAtText}"/></span>
                        </div>
                    </c:forEach>
                </div>
            </c:if>

            <c:if test="${not empty order.shipments}">
                <h2 style="margin-top:26px;">Vận chuyển</h2>
                <div class="cart-list">
                    <c:forEach var="sh" items="${order.shipments}">
                        <div class="order-line">
                            <span><b><c:out value="${sh.carrier}"/></b> · Mã theo dõi:
                                <c:out value="${sh.trackingNumber}"/> · <c:out value="${sh.status}"/></span>
                            <span class="muted">Gửi: <c:out value="${sh.shippedAtText}"/> · Nhận:
                                <c:out value="${sh.deliveredAtText}"/></span>
                        </div>
                    </c:forEach>
                </div>
            </c:if>
        </aside>
    </div>
</main>
<%@ include file="footer.jsp" %>
</body>
</html>
