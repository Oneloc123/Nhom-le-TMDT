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
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css?v=20261024-2">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css?v=20261024-2">
</head>
<body>
<%@ include file="header.jsp" %>

<main class="shop-page product-detail-page">
    <div id="productToast" class="product-toast" role="status" aria-live="polite" aria-atomic="true"></div>
    <div class="detail-layout product-hero-card">
        <div class="product-gallery">
        <div class="detail-visual product-visual ${product.visualClass}">
            <c:choose>
                <c:when test="${not empty product.imageUrl}">
                    <img id="mainProductImage" src="<c:out value='${product.imageUrl}'/>" alt="<c:out value='${product.name}'/>"
                         style="max-width:100%; max-height:100%; object-fit:contain;">
                </c:when>
                <c:otherwise>
                    <span><c:out value="${product.name}"/></span>
                </c:otherwise>
            </c:choose>
        </div>
        <c:if test="${fn:length(product.images) > 1}">
            <div class="product-thumbnails" aria-label="Thư viện ảnh sản phẩm">
                <c:forEach var="image" items="${product.images}" varStatus="status">
                    <button type="button" class="product-thumbnail ${status.first ? 'active' : ''}"
                            data-image="<c:out value='${image.imageUrl}'/>"
                            data-alt="<c:out value='${empty image.altText ? product.name : image.altText}'/>">
                        <img src="<c:out value='${image.imageUrl}'/>"
                             alt="<c:out value='${empty image.altText ? product.name : image.altText}'/>">
                    </button>
                </c:forEach>
            </div>
        </c:if>
        </div>
        <section class="detail-copy">
            <div class="eyebrow-line"><c:out value="${product.categoryName}"/></div>
            <h1><c:out value="${product.name}"/></h1>
            <div class="rating product-rating" aria-label="${product.rating} trên 5 sao">
                <c:forEach begin="1" end="5" var="star">
                    <span class="${star <= product.rating ? 'star-on' : 'star-off'}">★</span>
                </c:forEach>
                <span class="muted"><c:out value="${product.rating}"/>/5 · <c:out value="${product.reviewCount}"/> đánh giá</span>
            </div>
            <p class="description"><c:out value="${product.description}"/></p>
            <div class="detail-price">
                <c:choose>
                    <c:when test="${product.available}"><fmt:formatNumber value="${product.price}" type="number" groupingUsed="true"/>đ</c:when>
                    <c:otherwise><span class="unavailable-price">Tạm chưa có giá bán</span></c:otherwise>
                </c:choose>
            </div>
            <ul class="spec-list">
                <li><span>Phương pháp in</span><b><c:out value="${product.available ? product.printMethod : '—'}"/></b></li>
                <li><span>Số lượng tối thiểu</span><b><c:out value="${product.available ? product.minimumQuantity : '—'}"/><c:if test="${product.available}"> sản phẩm</c:if></b></li>
                <li><span>Gian hàng</span><b><c:out value="${product.available ? product.storeName : '—'}"/></b></li>
                <li><span>Đơn vị tính</span><b><c:out value="${product.baseUnit}"/></b></li>
                <li><span>Thương hiệu</span><b><c:out value="${empty product.brand ? 'Không có' : product.brand}"/></b></li>
                <li><span>Tùy chỉnh thiết kế</span><b>${product.customizable ? 'Có hỗ trợ' : 'Không hỗ trợ'}</b></li>
            </ul>
            <form class="product-purchase-panel js-cart-form" method="post"
                  action="${pageContext.request.contextPath}/cart"
                  data-checkout-url="${pageContext.request.contextPath}/order">
                <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                <input type="hidden" name="action" value="add">
                <c:choose>
                    <c:when test="${not product.available}">
                        <div class="purchase-unavailable">Sản phẩm hiện chưa có gian hàng còn hàng.</div>
                    </c:when>
                    <c:otherwise>
                        <input type="hidden" name="shopProductId" value="${product.primaryOffer.shopProductId}">
                        <label class="purchase-quantity">
                            <span>Số lượng</span>
                            <input class="quantity-input" type="number" name="quantity" min="${product.minimumQuantity}" value="${product.minimumQuantity}">
                        </label>
                        <div class="product-action-grid">
                            <button class="button-primary product-action-button" type="submit" data-cart-intent="cart">
                                <span class="button-label">Thêm vào giỏ hàng</span>
                            </button>
                            <button class="button-buy-now product-action-button" type="submit" name="next" value="/order" data-cart-intent="checkout">
                                <span class="button-label">Đặt ngay</span>
                            </button>
                        </div>
                    </c:otherwise>
                </c:choose>
            </form>
            <div class="product-secondary-actions">
                <c:choose>
                    <c:when test="${empty sessionScope.currentUser}">
                        <a class="favorite-button" href="${pageContext.request.contextPath}/login?redirect=/product-detail?id=${product.id}">
                            <span class="favorite-icon">♡</span><span>Yêu thích</span>
                        </a>
                    </c:when>
                    <c:otherwise>
                        <form id="favoriteForm" class="js-favorite-form" method="post" action="${pageContext.request.contextPath}/favorites">
                            <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                            <input type="hidden" name="action" value="${favorite ? 'remove' : 'add'}">
                            <input type="hidden" name="productId" value="${product.id}">
                            <input type="hidden" name="returnUrl" value="/product-detail?id=${product.id}">
                            <button class="favorite-button ${favorite ? 'is-favorite' : ''}" type="submit" aria-pressed="${favorite}">
                                <span class="favorite-icon">${favorite ? '♥' : '♡'}</span>
                                <span class="favorite-label">${favorite ? 'Bỏ yêu thích' : 'Yêu thích'}</span>
                            </button>
                        </form>
                    </c:otherwise>
                </c:choose>
                <c:if test="${product.customizable}">
                    <a class="custom-quote-link" href="${pageContext.request.contextPath}/quotes?productId=${product.id}">Yêu cầu tùy chỉnh / báo giá</a>
                </c:if>
            </div>
        </section>
    </div>

    <%-- Các chào bán (gian hàng) cho sản phẩm này --%>
    <c:if test="${not empty product.offers}">
        <section class="summary-panel offers-panel">
            <h2>Lựa chọn gian hàng (<c:out value="${fn:length(product.offers)}"/>)</h2>
            <p class="muted">Mỗi gian hàng có riêng giá, vật liệu và thời gian sản xuất. Chọn chào bán phù hợp để thêm vào giỏ.</p>
            <div class="cart-list">
                <c:forEach var="offer" items="${product.offers}">
                    <div class="cart-line offer-line">
                        <div>
                            <h3><c:out value="${offer.shopName}"/></h3>
                            <p><c:out value="${offer.printingMethod}"/> · <c:out value="${offer.material}"/> ·
                                từ <c:out value="${offer.minimumQuantity}"/> sản phẩm · <c:out value="${offer.productionDays}"/> ngày sản xuất</p>
                        </div>
                        <strong><fmt:formatNumber value="${offer.unitPrice}" type="number" groupingUsed="true"/>đ</strong>
                        <c:choose>
                            <c:when test="${offer.purchasable}">
                                <form class="js-cart-form offer-add-form" method="post" action="${pageContext.request.contextPath}/cart">
                                    <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                    <input type="hidden" name="action" value="add">
                                    <input type="hidden" name="shopProductId" value="${offer.shopProductId}">
                                    <input class="quantity-input" type="number" name="quantity" min="${offer.minimumQuantity}" value="${offer.minimumQuantity}" style="width:80px;">
                                    <button class="button-primary" type="submit" data-cart-intent="cart"><span class="button-label">Thêm</span></button>
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
        <h2>Đánh giá sản phẩm (<c:out value="${totalPublishedReviews}"/>)</h2>
        <c:choose>
            <c:when test="${empty reviews}">
                <div class="empty-state">Chưa có đánh giá nào cho sản phẩm này.</div>
            </c:when>
            <c:otherwise>
                <div class="cart-list">
                    <c:forEach var="review" items="${reviews}">
                        <div class="cart-line">
                            <div>
                                <h3><c:out value="${review.userDisplayName}"/>
                                    <span class="rating" aria-label="${review.rating} trên 5 sao">
                                        <c:out value="${review.stars}"/><span class="star-off"><c:out value="${review.emptyStars}"/></span>
                                    </span>
                                </h3>
                                <c:if test="${not empty review.createdAtText}">
                                    <small class="review-date"><c:out value="${review.createdAtText}"/></small>
                                </c:if>
                                <p><c:out value="${review.reviewText}"/></p>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
        <c:if test="${totalReviewPages > 1}">
            <nav class="pagination" aria-label="Phân trang đánh giá">
                <c:if test="${reviewPage > 1}">
                    <a class="button-secondary" href="${pageContext.request.contextPath}/product-detail?id=${product.id}&amp;reviewPage=${reviewPage - 1}#reviews">Trang trước</a>
                </c:if>
                <span>Trang <c:out value="${reviewPage}"/> / <c:out value="${totalReviewPages}"/></span>
                <c:if test="${reviewPage < totalReviewPages}">
                    <a class="button-secondary" href="${pageContext.request.contextPath}/product-detail?id=${product.id}&amp;reviewPage=${reviewPage + 1}#reviews">Trang sau</a>
                </c:if>
            </nav>
        </c:if>
    </section>

    <c:if test="${not empty relatedProducts}">
        <section class="related-products" aria-labelledby="related-title">
            <h2 id="related-title">Sản phẩm liên quan</h2>
            <div class="products-grid related-grid">
                <c:forEach var="related" items="${relatedProducts}">
                    <article class="product-card">
                        <a href="${pageContext.request.contextPath}/product-detail?id=${related.id}">
                            <div class="product-visual ${related.visualClass}">
                                <c:choose>
                                    <c:when test="${not empty related.imageUrl}"><img src="<c:out value='${related.imageUrl}'/>" alt="<c:out value='${related.name}'/>"></c:when>
                                    <c:otherwise><c:out value="${related.name}"/></c:otherwise>
                                </c:choose>
                            </div>
                        </a>
                        <div class="product-copy">
                            <h3><a href="${pageContext.request.contextPath}/product-detail?id=${related.id}"><c:out value="${related.name}"/></a></h3>
                            <div class="meta"><c:out value="${related.categoryName}"/></div>
                            <div class="product-price">
                                <c:choose>
                                    <c:when test="${related.available}"><fmt:formatNumber value="${related.price}" type="number" groupingUsed="true"/>đ</c:when>
                                    <c:otherwise>Chưa có giá</c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                    </article>
                </c:forEach>
            </div>
        </section>
    </c:if>
</main>
<%@ include file="footer.jsp" %>
<script defer src="${pageContext.request.contextPath}/js/product-detail.js?v=20261024-3"></script>
</body>
</html>
