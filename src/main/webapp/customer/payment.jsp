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
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css">
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
                    <span><b>Thanh toán khi nhận hàng (COD)</b><small>Trả tiền mặt khi đơn được giao đến tay</small></span>
                </label>
                <label class="payment-option" style="opacity:.6;">
                    <input type="radio" name="methodGateway" value="VNPay" disabled>
                    <span><b>VNPay</b><small>Chưa cấu hình cổng thanh toán (thiếu credentials nhà cung cấp)</small></span>
                </label>
                <label class="payment-option" style="opacity:.6;">
                    <input type="radio" name="methodGateway" value="MoMo" disabled>
                    <span><b>MoMo</b><small>Chưa cấu hình ví MoMo</small></span>
                </label>
                <label class="payment-option" style="opacity:.6;">
                    <input type="radio" name="methodGateway" value="Thẻ ngân hàng" disabled>
                    <span><b>Thẻ ngân hàng</b><small>Chưa cấu hình cổng thẻ</small></span>
                </label>
                <p class="muted">Dự án chưa tích hợp cổng thanh toán trực tuyến — các lựa chọn trên bị tắt
                    để không giả lập giao dịch thật. Đơn COD sẽ được xác nhận ngay và bạn trả tiền khi nhận hàng.</p>
                <button class="button-primary" type="submit">Xác nhận thanh toán COD
                    <fmt:formatNumber value="${order.totalAmount}" type="number" groupingUsed="true"/>đ</button>
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
</body>
</html>
