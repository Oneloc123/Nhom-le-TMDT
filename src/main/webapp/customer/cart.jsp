<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Giỏ hàng - PrintKraft</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css">
</head>
<body>
<%@ include file="header.jsp" %>

<main class="shop-page" id="cartPage" data-products-url="${pageContext.request.contextPath}/products">
    <div class="shop-heading">
        <div>
            <div class="eyebrow-line">Sản phẩm &amp; mua hàng</div>
            <h1>Quản lý giỏ hàng</h1>
            <p>Kiểm tra số lượng sản phẩm trước khi đặt hàng.</p>
        </div>
        <a class="button-secondary" href="${pageContext.request.contextPath}/products">Tiếp tục mua hàng</a>
    </div>

    <c:if test="${param.error == 'empty'}">
        <div class="notice-error">Giỏ hàng đang trống, hãy chọn sản phẩm trước.</div>
    </c:if>
    <div id="cartNotice" class="notice-ok" role="status" aria-live="polite" ${empty notice ? 'hidden' : ''}>
        <c:out value="${notice}"/>
    </div>
    <c:if test="${not empty sessionScope.currentUser and empty cartItems}">
        <p class="muted">Giỏ hàng được lưu theo tài khoản của bạn.</p>
    </c:if>

    <c:choose>
        <c:when test="${empty cartItems}">
            <div class="empty-state">
                <p>Chưa có sản phẩm nào trong giỏ hàng.</p>
                <a class="button-primary" href="${pageContext.request.contextPath}/products" style="margin-top:16px;">Xem sản phẩm</a>
            </div>
        </c:when>
        <c:otherwise>
            <div class="checkout-layout" id="cartContent">
                <section class="summary-panel">
                    <h2 id="cartQuantityHeading"><c:out value="${cartTotalQuantity}"/> sản phẩm</h2>
                    <div class="cart-list">
                        <c:forEach var="item" items="${cartItems}">
                            <div class="cart-line" data-cart-line="${item.shopProductId}">
                                <div>
                                    <h3><a href="${pageContext.request.contextPath}/product-detail?id=${item.product.id}"><c:out value="${item.product.name}"/></a></h3>
                                    <p><c:out value="${item.shopName}"/> · <c:out value="${item.product.printMethod}"/> · <c:out value="${item.material}"/>
                                        · <fmt:formatNumber value="${item.unitPrice}" type="number" groupingUsed="true"/>đ/sản phẩm
                                        · tối thiểu <c:out value="${item.minimumQuantity}"/></p>
                                </div>
                                <form class="quantity-form js-cart-mutation" method="post" action="${pageContext.request.contextPath}/cart">
                                    <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                    <input type="hidden" name="action" value="update">
                                    <input type="hidden" name="shopProductId" value="${item.shopProductId}">
                                    <input class="quantity-input" type="number" name="quantity"
                                           min="${item.minimumQuantity > 0 ? item.minimumQuantity : 1}"
                                           max="100000" value="${item.quantity}" aria-label="Số lượng">
                                    <button class="button-secondary" type="submit">Cập nhật</button>
                                </form>
                                <strong data-line-subtotal><fmt:formatNumber value="${item.subtotal}" type="number" groupingUsed="true"/>đ</strong>
                                <form class="js-cart-mutation" method="post" action="${pageContext.request.contextPath}/cart"
                                      data-confirm="Xóa sản phẩm này khỏi giỏ hàng?">
                                    <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                    <input type="hidden" name="action" value="remove">
                                    <input type="hidden" name="shopProductId" value="${item.shopProductId}">
                                    <button class="button-danger" type="submit">Xóa</button>
                                </form>
                            </div>
                        </c:forEach>
                    </div>
                    <form class="js-cart-mutation" method="post" action="${pageContext.request.contextPath}/cart"
                          data-confirm="Xóa toàn bộ sản phẩm trong giỏ hàng?" style="margin-top:18px;">
                        <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                        <input type="hidden" name="action" value="clear">
                        <button class="button-secondary" type="submit">Xóa toàn bộ giỏ hàng</button>
                    </form>
                </section>
                <aside class="order-panel">
                    <h2>Tạm tính</h2>
                    <div class="summary-line">
                        <span>Tổng tiền sản phẩm</span>
                        <strong class="cart-total" id="cartSubtotal"><fmt:formatNumber value="${cartSubtotal}" type="number" groupingUsed="true"/>đ</strong>
                    </div>
                    <p class="muted" style="margin:14px 0 20px;">Phí vận chuyển sẽ được xác nhận ở bước đặt hàng.</p>
                    <a class="button-primary" style="width:100%;" href="${pageContext.request.contextPath}/order">Tiến hành đặt hàng</a>
                </aside>
            </div>
        </c:otherwise>
    </c:choose>
</main>
<%@ include file="footer.jsp" %>
<script defer src="${pageContext.request.contextPath}/js/cart.js?v=20261025-1"></script>
</body>
</html>
