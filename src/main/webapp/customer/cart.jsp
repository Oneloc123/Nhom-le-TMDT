<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Giỏ hàng | PrintKraft</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css?v=20261104-account-layout">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/user-v2.css?v=20261104-account-layout">
</head>
<body>
<%@ include file="header.jsp" %>

<main class="uv2-page uv2-cart-page" id="cartPage" data-products-url="${pageContext.request.contextPath}/products">
    <div class="uv2-breadcrumb">
        <a href="${pageContext.request.contextPath}/home">Trang chủ</a><span>/</span>
        <a href="${pageContext.request.contextPath}/account">Tài khoản</a><span>/</span><span>Giỏ hàng</span>
    </div>
    <div class="uv2-page-heading">
        <div>
            <p class="uv2-eyebrow">SẢN PHẨM &amp; MUA HÀNG</p>
            <h1>Giỏ hàng của bạn</h1>
            <p>Kiểm tra sản phẩm, xưởng in và số lượng trước khi đặt hàng.</p>
        </div>
        <a class="uv2-button is-light" href="${pageContext.request.contextPath}/products">Tiếp tục mua hàng</a>
    </div>

    <div class="uv2-account-grid">
        <jsp:include page="/WEB-INF/views/user/account-sidebar.jsp"/>
        <div class="uv2-content uv2-cart-content">
            <c:if test="${param.error == 'empty'}">
                <div class="uv2-alert is-error">Giỏ hàng đang trống, hãy chọn sản phẩm trước.</div>
            </c:if>
            <div id="cartNotice" class="notice-ok uv2-cart-notice" role="status" aria-live="polite" ${empty notice ? 'hidden' : ''}>
                <c:out value="${notice}"/>
            </div>
            <c:if test="${not empty sessionScope.currentUser and empty cartItems}">
                <p class="uv2-muted">Giỏ hàng được lưu theo tài khoản của bạn.</p>
            </c:if>

            <c:choose>
                <c:when test="${empty cartItems}">
                    <section class="uv2-panel uv2-empty uv2-cart-empty" id="cartContent">
                        <div class="uv2-empty-icon" aria-hidden="true">🛒</div>
                        <b>Giỏ hàng đang trống</b>
                        <p>Hãy khám phá sản phẩm và lựa chọn xưởng in phù hợp với nhu cầu của bạn.</p>
                        <a class="uv2-button is-primary" href="${pageContext.request.contextPath}/products">Khám phá sản phẩm</a>
                    </section>
                </c:when>
                <c:otherwise>
                    <div class="checkout-layout uv2-checkout-layout" id="cartContent">
                        <section class="summary-panel uv2-panel uv2-cart-items-panel">
                            <div class="uv2-panel-head">
                                <div>
                                    <p class="uv2-eyebrow">SẢN PHẨM ĐÃ CHỌN</p>
                                    <h2 id="cartQuantityHeading"><c:out value="${cartTotalQuantity}"/> sản phẩm</h2>
                                </div>
                                <span class="uv2-cart-secure">Giá được xác nhận khi đặt hàng</span>
                            </div>
                            <div class="cart-list">
                                <c:forEach var="item" items="${cartItems}">
                                    <article class="cart-line uv2-cart-line" data-cart-line="${item.shopProductId}">
                                        <div class="uv2-cart-item-info">
                                            <h3><a href="${pageContext.request.contextPath}/product-detail?id=${item.product.id}"><c:out value="${item.product.name}"/></a></h3>
                                            <p><c:out value="${item.shopName}"/> · <c:out value="${item.product.printMethod}"/> · <c:out value="${item.material}"/></p>
                                            <small><fmt:formatNumber value="${item.unitPrice}" type="number" groupingUsed="true"/>đ/sản phẩm · Số lượng tối thiểu <c:out value="${item.minimumQuantity}"/></small>
                                        </div>
                                        <form class="quantity-form js-cart-mutation uv2-quantity-form" method="post" action="${pageContext.request.contextPath}/cart">
                                            <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                            <input type="hidden" name="action" value="update">
                                            <input type="hidden" name="shopProductId" value="${item.shopProductId}">
                                            <label class="uv2-quantity-label">Số lượng
                                                <input class="quantity-input" type="number" name="quantity"
                                                       min="${item.minimumQuantity > 0 ? item.minimumQuantity : 1}"
                                                       max="100000" value="${item.quantity}" aria-label="Số lượng" required>
                                            </label>
                                            <button class="uv2-button is-light" type="submit">Cập nhật</button>
                                        </form>
                                        <strong class="uv2-cart-line-total" data-line-subtotal><fmt:formatNumber value="${item.subtotal}" type="number" groupingUsed="true"/>đ</strong>
                                        <form class="js-cart-mutation uv2-cart-remove" method="post" action="${pageContext.request.contextPath}/cart"
                                              data-confirm="Xóa sản phẩm này khỏi giỏ hàng?">
                                            <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                            <input type="hidden" name="action" value="remove">
                                            <input type="hidden" name="shopProductId" value="${item.shopProductId}">
                                            <button class="button-danger" type="submit">Xóa</button>
                                        </form>
                                    </article>
                                </c:forEach>
                            </div>
                            <form class="js-cart-mutation uv2-clear-cart" method="post" action="${pageContext.request.contextPath}/cart"
                                  data-confirm="Xóa toàn bộ sản phẩm trong giỏ hàng?" data-confirm-title="Xóa toàn bộ giỏ hàng?">
                                <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                <input type="hidden" name="action" value="clear">
                                <button class="uv2-button is-light" type="submit">Xóa toàn bộ giỏ hàng</button>
                            </form>
                        </section>
                        <aside class="order-panel uv2-panel uv2-cart-total-panel">
                            <p class="uv2-eyebrow">TÓM TẮT ĐƠN HÀNG</p>
                            <h2>Tạm tính</h2>
                            <div class="summary-line uv2-cart-summary-line">
                                <span>Tổng tiền sản phẩm</span>
                                <strong class="cart-total" id="cartSubtotal"><fmt:formatNumber value="${cartSubtotal}" type="number" groupingUsed="true"/>đ</strong>
                            </div>
                            <p class="muted uv2-cart-shipping-note">Phí vận chuyển, thông tin giao nhận và phương thức thanh toán sẽ được xác nhận ở bước đặt hàng.</p>
                            <a class="button-primary uv2-button is-primary uv2-checkout-button" href="${pageContext.request.contextPath}/order">Tiến hành đặt hàng</a>
                            <div class="uv2-cart-trust-note"><span aria-hidden="true">✓</span> Thông tin đơn hàng được kiểm tra trước khi xác nhận.</div>
                        </aside>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</main>

<div class="cart-confirm-modal" id="cartConfirmModal" hidden>
    <div class="cart-confirm-backdrop" data-confirm-cancel></div>
    <section class="cart-confirm-dialog" role="dialog" aria-modal="true"
             aria-labelledby="cartConfirmTitle" aria-describedby="cartConfirmMessage" tabindex="-1">
        <div class="cart-confirm-icon" aria-hidden="true">!</div>
        <div class="cart-confirm-copy">
            <h2 id="cartConfirmTitle">Xóa sản phẩm?</h2>
            <p id="cartConfirmMessage">Xóa sản phẩm này khỏi giỏ hàng?</p>
        </div>
        <div class="cart-confirm-actions">
            <button class="button-secondary" type="button" data-confirm-cancel>Giữ lại</button>
            <button class="button-danger cart-confirm-submit" type="button" id="cartConfirmSubmit">Xóa</button>
        </div>
    </section>
</div>
<%@ include file="footer.jsp" %>
<script defer src="${pageContext.request.contextPath}/js/cart.js?v=20261104-account-layout"></script>
</body>
</html>