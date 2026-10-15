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
            <p>Chọn địa chỉ nhận hàng để tạo đơn. Giá được tính lại từ hệ thống.</p>
        </div>
    </div>

    <c:if test="${not empty error}">
        <div class="notice-error"><c:out value="${error}"/></div>
    </c:if>

    <div class="checkout-layout">
        <section class="form-panel">
            <h2>Thông tin nhận hàng</h2>
            <form class="stack-form" method="post" action="${pageContext.request.contextPath}/order">
                <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">

                <c:if test="${not empty addresses}">
                    <div class="field">
                        <span>Địa chỉ đã lưu</span>
                        <c:forEach var="addr" items="${addresses}" varStatus="loop">
                            <label style="display:block; margin:6px 0;">
                                <input type="radio" name="addressId" value="${addr.addressId}"
                                       ${loop.first ? 'checked' : ''}>
                                <b><c:out value="${addr.recipientName}"/> — <c:out value="${addr.recipientPhone}"/></b>
                                <c:if test="${addr.defaultAddress}"><span class="product-tag">Mặc định</span></c:if><br>
                                <span class="muted"><c:out value="${addr.fullLine}"/></span>
                            </label>
                        </c:forEach>
                        <label style="display:block; margin:6px 0;">
                            <input type="radio" name="addressId" value="" onchange="document.getElementById('manualAddress').style.display='block'">
                            Giao đến địa chỉ khác (nhập bên dưới)
                        </label>
                    </div>
                </c:if>

                <div class="field" id="manualAddress">
                    <label for="fullName">Người nhận</label>
                    <input id="fullName" type="text" value="<c:out value='${user.fullName}'/>" disabled>
                </div>
                <div class="field">
                    <label for="phone">Số điện thoại (khi nhập địa chỉ khác)</label>
                    <input id="phone" type="text" name="phone" value="<c:out value='${user.phone}'/>"
                           pattern="[0-9]{9,12}">
                </div>
                <div class="field">
                    <label for="address">Địa chỉ nhận hàng (khi nhập địa chỉ khác)</label>
                    <textarea id="address" name="address"></textarea>
                </div>
                <p class="muted">Chọn một địa chỉ đã lưu ở trên HOẶC nhập địa chỉ khác. Phí vận chuyển
                    <fmt:formatNumber value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.service.OrderService.SHIPPING_FEE %>"
                                      type="number" groupingUsed="true"/>đ — miễn phí từ
                    <fmt:formatNumber value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.service.OrderService.FREE_SHIPPING_THRESHOLD %>"
                                      type="number" groupingUsed="true"/>đ.</p>
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
                        <span><c:out value="${item.product.name}"/> × <c:out value="${item.quantity}"/>
                            <span class="muted">(<c:out value="${item.shopName}"/>)</span></span>
                        <strong><fmt:formatNumber value="${item.subtotal}" type="number" groupingUsed="true"/>đ</strong>
                    </div>
                </c:forEach>
            </div>
            <div class="summary-line" style="margin-top:22px;">
                <strong>Tạm tính</strong>
                <strong class="cart-total"><fmt:formatNumber value="${cartSubtotal}" type="number" groupingUsed="true"/>đ</strong>
            </div>
        </aside>
    </div>
</main>
<%@ include file="footer.jsp" %>
</body>
</html>
