<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Thanh toán trực tuyến - PrintKraft</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css">
</head>
<body>
<%@ include file="header.jsp" %>

<main class="shop-page">
    <div class="shop-heading">
        <div>
            <div class="eyebrow-line">Bước 2 trong 2</div>
            <h1>Thanh toán trực tuyến</h1>
            <p>Chọn phương thức thanh toán cho đơn <b><c:out value="${order.orderNumber}"/></b>.</p>
        </div>
    </div>

    <c:if test="${not empty error}">
        <div class="notice-error"><c:out value="${error}"/></div>
    </c:if>

    <div class="checkout-layout">
        <section class="form-panel">
            <h2>Phương thức thanh toán</h2>
            <form class="stack-form" method="post" action="${pageContext.request.contextPath}/payment">
                <input type="hidden" name="orderNumber" value="<c:out value='${order.orderNumber}'/>">
                <label class="payment-option">
                    <input type="radio" name="method" value="VNPay" checked>
                    <span><b>VNPay</b><small>Thanh toán qua cổng VNPay</small></span>
                </label>
                <label class="payment-option">
                    <input type="radio" name="method" value="MoMo">
                    <span><b>MoMo</b><small>Thanh toán qua ví điện tử MoMo</small></span>
                </label>
                <label class="payment-option">
                    <input type="radio" name="method" value="Thẻ ngân hàng">
                    <span><b>Thẻ ngân hàng</b><small>Visa, Mastercard hoặc thẻ nội địa</small></span>
                </label>
                <p class="muted">Đây là luồng mô phỏng để kiểm thử giao diện và nghiệp vụ. Không phát sinh giao dịch thật.</p>
                <button class="button-primary" type="submit">Thanh toán <fmt:formatNumber value="${order.total}" type="number" groupingUsed="true"/>đ</button>
            </form>
        </section>
        <aside class="order-panel">
            <h2>Thông tin đơn hàng</h2>
            <div class="summary-line"><span>Mã đơn</span><b><c:out value="${order.orderNumber}"/></b></div>
            <div class="summary-line" style="margin-top:12px;"><span>Người nhận</span><b><c:out value="${order.customerName}"/></b></div>
            <div class="summary-line" style="margin-top:12px;"><span>Số sản phẩm</span><b><c:out value="${order.itemCount}"/></b></div>
            <div class="summary-line" style="margin-top:22px;"><strong>Tổng cộng</strong><strong class="cart-total"><fmt:formatNumber value="${order.total}" type="number" groupingUsed="true"/>đ</strong></div>
        </aside>
    </div>
</main>
<%@ include file="footer.jsp" %>
</body>
</html>
