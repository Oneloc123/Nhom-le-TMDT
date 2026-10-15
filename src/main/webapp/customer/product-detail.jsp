<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><c:out value="${product.name}"/> - PrintKraft</title>
    <%-- tiêu đề đã encode bằng c:out --%>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css">
</head>
<body>
<%@ include file="header.jsp" %>

<main class="shop-page">
    <div class="detail-layout">
        <div class="detail-visual product-visual ${product.visualClass}">
            <c:choose>
                <c:when test="${not empty product.imageUrl}">
                    <img src="<c:out value='${product.imageUrl}'/>" alt="<c:out value='${product.name}'/>"
                         style="max-width:100%; max-height:100%; object-fit:contain;">
                </c:when>
                <c:otherwise>
                    <span><c:out value="${product.name}"/></span>
                </c:otherwise>
            </c:choose>
        </div>
        <section class="detail-copy">
            <div class="eyebrow-line"><c:out value="${product.categoryName}"/></div>
            <h1><c:out value="${product.name}"/></h1>
            <div class="rating">★★★★★ <span class="muted"><c:out value="${product.rating}"/>/5 · <c:out value="${product.reviewCount}"/> đánh giá</span></div>
            <p class="description"><c:out value="${product.description}"/></p>
            <div class="detail-price"><fmt:formatNumber value="${product.price}" type="number" groupingUsed="true"/>đ</div>
            <ul class="spec-list">
                <li><span>Phương pháp in</span><b><c:out value="${product.printMethod}"/></b></li>
                <li><span>Số lượng tối thiểu</span><b><c:out value="${product.minimumQuantity}"/> sản phẩm</b></li>
                <li><span>Gian hàng</span><b><c:out value="${product.storeName}"/></b></li>
                <li><span>Đơn vị tính</span><b><c:out value="${product.baseUnit}"/></b></li>
            </ul>
            <form class="button-row" method="post" action="${pageContext.request.contextPath}/cart">
                <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                <input type="hidden" name="action" value="add">
                <c:choose>
                    <c:when test="${empty product.offers}">
                        <p class="muted">Sản phẩm hiện chưa có gian hàng nhận in.</p>
                    </c:when>
                    <c:otherwise>
                        <input type="hidden" name="productId" value="${product.id}">
                        <label class="field" style="margin:0; width:100px;">
                            <span>Số lượng</span>
                            <input class="quantity-input" type="number" name="quantity" min="${product.minimumQuantity}" value="${product.minimumQuantity}">
                        </label>
                        <button class="button-primary" type="submit">Thêm vào giỏ hàng</button>
                    </c:otherwise>
                </c:choose>
                <a class="button-secondary" href="${pageContext.request.contextPath}/products">Tiếp tục mua</a>
            </form>
            <%-- Yêu thích: thêm/bỏ lưu theo tài khoản hiện tại --%>
            <c:choose>
                <c:when test="${empty sessionScope.currentUser}">
                    <p class="muted"><a href="${pageContext.request.contextPath}/login">Đăng nhập</a> để lưu sản phẩm yêu thích.</p>
                </c:when>
                <c:when test="${favorite}">
                    <form method="post" action="${pageContext.request.contextPath}/favorites" style="margin-top:10px;">
                        <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                        <input type="hidden" name="action" value="remove">
                        <input type="hidden" name="productId" value="${product.id}">
                        <button class="button-secondary" type="submit">♥ Đang lưu yêu thích</button>
                    </form>
                </c:when>
                <c:otherwise>
                    <form method="post" action="${pageContext.request.contextPath}/favorites" style="margin-top:10px;">
                        <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                        <input type="hidden" name="action" value="add">
                        <input type="hidden" name="productId" value="${product.id}">
                        <button class="button-secondary" type="submit">♡ Thêm vào yêu thích</button>
                    </form>
                </c:otherwise>
            </c:choose>
        </section>
    </div>

    <%-- Các chào bán (gian hàng) cho sản phẩm này --%>
    <c:if test="${not empty product.offers}">
        <section class="summary-panel" style="margin-top:32px;">
            <h2>Lựa chọn gian hàng (<c:out value="${fn:length(product.offers)}"/>)</h2>
            <p class="muted">Mỗi gian hàng có riêng giá, vật liệu và thời gian sản xuất. Chọn chào bán phù hợp để thêm vào giỏ.</p>
            <div class="cart-list">
                <c:forEach var="offer" items="${product.offers}">
                    <div class="cart-line">
                        <div>
                            <h3><c:out value="${offer.shopName}"/></h3>
                            <p>In <c:out value="${offer.printingMethod}"/> · <c:out value="${offer.material}"/> ·
                                từ <c:out value="${offer.minimumQuantity}"/> sản phẩm · <c:out value="${offer.productionDays}"/> ngày sản xuất</p>
                        </div>
                        <strong><fmt:formatNumber value="${offer.unitPrice}" type="number" groupingUsed="true"/>đ</strong>
                        <c:choose>
                            <c:when test="${offer.purchasable}">
                                <form method="post" action="${pageContext.request.contextPath}/cart">
                                    <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                    <input type="hidden" name="action" value="add">
                                    <input type="hidden" name="shopProductId" value="${offer.shopProductId}">
                                    <input class="quantity-input" type="number" name="quantity" min="${offer.minimumQuantity}" value="${offer.minimumQuantity}" style="width:80px;">
                                    <button class="button-primary" type="submit">Thêm</button>
                                </form>
                            </c:when>
                            <c:otherwise>
                                <span class="muted">Tạm hết hàng</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </c:forEach>
            </div>
        </section>
    </c:if>

    <%-- Đánh giá đã đăng của khách đã mua --%>
    <section class="summary-panel" id="reviews" style="margin-top:32px;">
        <h2>Đánh giá sản phẩm</h2>
        <c:choose>
            <c:when test="${empty reviews}">
                <div class="empty-state">Chưa có đánh giá nào cho sản phẩm này.</div>
            </c:when>
            <c:otherwise>
                <div class="cart-list">
                    <c:forEach var="review" items="${reviews}">
                        <div class="cart-line">
                            <div>
                                <h3><c:out value="${review.userDisplayName}"/> <span class="rating">★★★★★</span></h3>
                                <p><c:out value="${review.reviewText}"/></p>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </section>
</main>
<%@ include file="footer.jsp" %>
</body>
</html>
