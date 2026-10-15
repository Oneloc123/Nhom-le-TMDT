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

<main class="shop-page">
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

    <c:choose>
        <c:when test="${empty cartItems}">
            <div class="empty-state">
                <p>Chưa có sản phẩm nào trong giỏ hàng.</p>
                <a class="button-primary" href="${pageContext.request.contextPath}/products" style="margin-top:16px;">Xem sản phẩm</a>
            </div>
        </c:when>
        <c:otherwise>
            <div class="checkout-layout">
                <section class="summary-panel">
                    <h2><c:out value="${cartTotalQuantity}"/> sản phẩm</h2>
                    <div class="cart-list">
                        <c:forEach var="item" items="${cartItems}">
                            <div class="cart-line">
                                <div>
                                    <h3><a href="${pageContext.request.contextPath}/product-detail?id=${item.product.id}"><c:out value="${item.product.name}"/></a></h3>
                                    <p><c:out value="${item.product.printMethod}"/> · <fmt:formatNumber value="${item.product.price}" type="number" groupingUsed="true"/>đ/sản phẩm</p>
                                </div>
                                <form class="quantity-form" method="post" action="${pageContext.request.contextPath}/cart">
                                    <input type="hidden" name="action" value="update">
                                    <input type="hidden" name="productId" value="${item.product.id}">
                                    <input class="quantity-input" type="number" name="quantity" min="0" value="${item.quantity}" aria-label="Số lượng">
                                    <button class="button-secondary" type="submit">Cập nhật</button>
                                </form>
                                <strong><fmt:formatNumber value="${item.subtotal}" type="number" groupingUsed="true"/>đ</strong>
                                <form method="post" action="${pageContext.request.contextPath}/cart">
                                    <input type="hidden" name="action" value="remove">
                                    <input type="hidden" name="productId" value="${item.product.id}">
                                    <button class="button-danger" type="submit">Xóa</button>
                                </form>
                            </div>
                        </c:forEach>
                    </div>
                    <form method="post" action="${pageContext.request.contextPath}/cart" style="margin-top:18px;">
                        <input type="hidden" name="action" value="clear">
                        <button class="button-secondary" type="submit">Xóa toàn bộ giỏ hàng</button>
                    </form>
                </section>
                <aside class="order-panel">
                    <h2>Tạm tính</h2>
                    <div class="summary-line">
                        <span>Tổng tiền sản phẩm</span>
                        <strong class="cart-total"><fmt:formatNumber value="${cartSubtotal}" type="number" groupingUsed="true"/>đ</strong>
                    </div>
                    <p class="muted" style="margin:14px 0 20px;">Phí vận chuyển sẽ được xác nhận ở bước đặt hàng.</p>
                    <a class="button-primary" style="width:100%;" href="${pageContext.request.contextPath}/order">Tiến hành đặt hàng</a>
                </aside>
            </div>
        </c:otherwise>
    </c:choose>
</main>
<%@ include file="footer.jsp" %>
</body>
</html>
