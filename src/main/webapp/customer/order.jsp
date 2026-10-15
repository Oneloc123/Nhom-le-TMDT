<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đặt hàng - PrintKraft</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css">
</head>
<body>
<%@ include file="header.jsp" %>

<main class="shop-page">
    <div class="shop-heading">
        <div>
            <div class="eyebrow-line">Bước 1 trong 2</div>
            <h1>Đặt hàng</h1>
            <p>Nhập thông tin nhận hàng để tạo đơn.</p>
        </div>
    </div>

    <c:if test="${not empty error}">
        <div class="notice-error"><c:out value="${error}"/></div>
    </c:if>

    <div class="checkout-layout">
        <section class="form-panel">
            <h2>Thông tin nhận hàng</h2>
            <form class="stack-form" method="post" action="${pageContext.request.contextPath}/order">
                <div class="field">
                    <label for="fullName">Người nhận</label>
                    <input id="fullName" type="text" value="<c:out value='${user.fullName}'/>" disabled>
                </div>
                <div class="field">
                    <label for="phone">Số điện thoại</label>
                    <input id="phone" type="text" name="phone" value="<c:out value='${user.phone}'/>" required>
                </div>
                <div class="field">
                    <label for="address">Địa chỉ nhận hàng</label>
                    <textarea id="address" name="address" required><c:out value="${user.address}"/></textarea>
                </div>
                <div class="button-row">
                    <button class="button-primary" type="submit">Xác nhận đặt hàng</button>
                    <a class="button-secondary" href="${pageContext.request.contextPath}/cart">Quay lại giỏ hàng</a>
                </div>
            </form>
        </section>
        <aside class="order-panel">
            <h2>Tóm tắt đơn hàng</h2>
            <div class="cart-list">
                <c:forEach var="item" items="${cartItems}">
                    <div class="order-line">
                        <span><c:out value="${item.product.name}"/> × <c:out value="${item.quantity}"/></span>
                        <strong><fmt:formatNumber value="${item.subtotal}" type="number" groupingUsed="true"/>đ</strong>
                    </div>
                </c:forEach>
            </div>
            <div class="summary-line" style="margin-top:22px;">
                <strong>Tổng thanh toán</strong>
                <strong class="cart-total"><fmt:formatNumber value="${cartSubtotal}" type="number" groupingUsed="true"/>đ</strong>
            </div>
        </aside>
    </div>
</main>
<%@ include file="footer.jsp" %>
</body>
</html>
