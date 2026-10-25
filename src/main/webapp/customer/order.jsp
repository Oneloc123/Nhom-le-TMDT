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
                                       ${(param.addressId == addr.addressId || (empty param.addressId && empty param.addressLine && loop.first)) ? 'checked' : ''}>
                                <b><c:out value="${addr.recipientName}"/> — <c:out value="${addr.recipientPhone}"/></b>
                                <c:if test="${addr.defaultAddress}"><span class="product-tag">Mặc định</span></c:if><br>
                                <span class="muted"><c:out value="${addr.fullLine}"/></span>
                            </label>
                        </c:forEach>
                        <label style="display:block; margin:6px 0;">
                            <input type="radio" name="addressId" value="" ${not empty param.addressLine ? 'checked' : ''}>
                            Giao đến địa chỉ khác (nhập bên dưới)
                        </label>
                    </div>
                </c:if>

                <div id="manualAddress">
                    <div class="field">
                        <label for="fullName">Người nhận</label>
                        <input id="fullName" type="text" name="recipientName" maxlength="120"
                               autocomplete="name" placeholder="Nhập họ và tên người nhận"
                               value="<c:out value='${not empty param.recipientName ? param.recipientName : user.fullName}'/>">
                    </div>
                    <div class="field">
                        <label for="phone">Số điện thoại</label>
                        <input id="phone" type="text" name="phone"
                               value="<c:out value='${not empty param.phone ? param.phone : user.phone}'/>"
                               inputmode="numeric" autocomplete="tel" placeholder="Ví dụ: 0901234567"
                               pattern="[0-9]{9,12}">
                    </div>
                    <div class="field">
                        <label for="province">Tỉnh / Thành phố</label>
                        <input id="province" type="text" name="province" maxlength="100"
                               autocomplete="address-level1" placeholder="Ví dụ: TP. Hồ Chí Minh"
                               value="<c:out value='${param.province}'/>">
                    </div>
                    <div class="field">
                        <label for="district">Quận / Huyện</label>
                        <input id="district" type="text" name="district" maxlength="100"
                               autocomplete="address-level2" placeholder="Ví dụ: Quận 1"
                               value="<c:out value='${param.district}'/>">
                    </div>
                    <div class="field">
                        <label for="addressLine">Địa chỉ cụ thể</label>
                        <input id="addressLine" type="text" name="addressLine" maxlength="255"
                               autocomplete="street-address" placeholder="Số nhà, tên đường, phường/xã"
                               value="<c:out value='${param.addressLine}'/>">
                    </div>
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
                <span>Tạm tính</span>
                <strong><fmt:formatNumber value="${cartSubtotal}" type="number" groupingUsed="true"/>đ</strong>
            </div>
            <div class="summary-line" style="margin-top:10px;">
                <span>Phí vận chuyển</span>
                <strong><fmt:formatNumber value="${shippingFee}" type="number" groupingUsed="true"/>đ</strong>
            </div>
            <div class="summary-line checkout-grand-total" style="margin-top:16px;">
                <strong>Tổng thanh toán</strong>
                <strong class="cart-total"><fmt:formatNumber value="${orderTotal}" type="number" groupingUsed="true"/>đ</strong>
            </div>
            <p class="muted" style="margin-top:12px;">Tổng tiền sẽ được kiểm tra lại từ dữ liệu mới nhất khi tạo đơn.</p>
        </aside>
    </div>
</main>
<%@ include file="footer.jsp" %>
<script defer src="${pageContext.request.contextPath}/js/order.js?v=20261025-1"></script>
</body>
</html>
