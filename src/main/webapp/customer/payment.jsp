<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Thanh toán - PrintKraft</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css?v=20261104-1">
</head>
<body>
<%@ include file="header.jsp" %>

<main class="shop-page">
    <div class="shop-heading">
        <div>
            <div class="eyebrow-line">Bước 2 trong 2</div>
            <h1>Thanh toán</h1>
            <p>Chọn phương thức thanh toán cho đơn <b><c:out value="${order.orderCode}"/></b>.</p>
        </div>
    </div>

    <c:if test="${not empty error}">
        <div class="notice-error"><c:out value="${error}"/></div>
    </c:if>

    <div class="checkout-layout">
        <section class="form-panel">
            <h2>Phương thức thanh toán</h2>
            <form class="stack-form" method="post" action="${pageContext.request.contextPath}/payment">
                <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                <input type="hidden" name="orderNumber" value="<c:out value='${order.orderCode}'/>">
                <label class="payment-option">
                    <input type="radio" name="method" value="COD" checked>
                    <span class="payment-option-copy"><b>Thanh toán khi nhận hàng (COD)</b><small>Trả tiền mặt khi đơn được giao đến tay</small></span>
                </label>
                <label class="payment-option ${vnpayConfigured ? '' : 'is-disabled'}">
                    <input type="radio" name="method" value="VNPay" ${vnpayConfigured ? '' : 'disabled'}>
                    <span class="payment-option-copy">
                        <span class="payment-title"><b>Thanh toán qua VNPAY</b><span class="vnpay-badge">VNPAY</span></span>
                        <small>
                            <c:choose>
                                <c:when test="${vnpayConfigured}">Thanh toán trực tuyến an toàn qua ngân hàng hoặc ứng dụng VNPAY</c:when>
                                <c:otherwise>Chưa cấu hình mã website và khóa bí mật VNPAY trên máy chủ</c:otherwise>
                            </c:choose>
                        </small>
                    </span>
                </label>
                <label class="payment-option is-disabled">
                    <input type="radio" name="methodGateway" value="MoMo" disabled>
                    <span class="payment-option-copy"><b>MoMo</b><small>Chưa cấu hình ví MoMo</small></span>
                </label>
                <label class="payment-option is-disabled">
                    <input type="radio" name="methodGateway" value="Thẻ ngân hàng" disabled>
                    <span class="payment-option-copy"><b>Thẻ ngân hàng</b><small>Chưa cấu hình cổng thẻ</small></span>
                </label>
                <p class="muted">Với VNPAY, bạn sẽ được chuyển sang cổng thanh toán và quay lại PrintKraft sau khi hoàn tất.</p>
                <button class="button-primary payment-submit" type="submit">
                    <span id="paymentActionText">Xác nhận đơn COD</span>
                    <span><fmt:formatNumber value="${order.totalAmount}" type="number" groupingUsed="true"/>đ</span>
                </button>
            </form>
        </section>
        <aside class="order-panel">
            <h2>Thông tin đơn hàng</h2>
            <div class="summary-line"><span>Mã đơn</span><b><c:out value="${order.orderCode}"/></b></div>
            <div class="summary-line" style="margin-top:12px;"><span>Người nhận</span><b><c:out value="${order.recipientName}"/></b></div>
            <div class="summary-line" style="margin-top:12px;"><span>Số điện thoại</span><b><c:out value="${order.recipientPhone}"/></b></div>
            <div class="summary-line" style="margin-top:12px;"><span>Giao đến</span><b><c:out value="${order.shippingAddress}"/></b></div>
            <div class="summary-line" style="margin-top:12px;"><span>Số sản phẩm</span><b><c:out value="${order.itemCount}"/></b></div>
            <div class="summary-line" style="margin-top:12px;"><span>Phí vận chuyển</span>
                <b><fmt:formatNumber value="${order.shippingFee}" type="number" groupingUsed="true"/>đ</b></div>
            <div class="summary-line" style="margin-top:22px;"><strong>Tổng cộng</strong>
                <strong class="cart-total"><fmt:formatNumber value="${order.totalAmount}" type="number" groupingUsed="true"/>đ</strong></div>
        </aside>
    </div>
</main>
<%@ include file="footer.jsp" %>
<script defer src="${pageContext.request.contextPath}/js/payment.js?v=20261104-1"></script>
</body>
</html>
