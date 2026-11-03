<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sản phẩm - PrintKraft</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css?v=20261024-3">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css">
</head>
<body>
<%@ include file="header.jsp" %>

<main class="shop-page">
    <div class="shop-heading">
        <div>
            <div class="eyebrow-line">Sản phẩm &amp; mua hàng</div>
            <c:choose>
                <c:when test="${not empty storeName}">
                    <h1>Sản phẩm của <c:out value="${storeName}"/></h1>
                </c:when>
                <c:otherwise><h1>Chọn sản phẩm phù hợp</h1></c:otherwise>
            </c:choose>
            <p>Khám phá các sản phẩm in theo yêu cầu từ những gian hàng được đánh giá cao.</p>
        </div>
        <a class="button-secondary" href="${pageContext.request.contextPath}/stores">Tìm gian hàng</a>
    </div>

    <c:if test="${not empty filterError}">
        <div class="notice-error"><c:out value="${filterError}"/></div>
    </c:if>
    <c:if test="${not empty storeName}">
        <div class="filter-context">
            <span>Đang xem chào bán của <strong><c:out value="${storeName}"/></strong>.</span>
            <a href="${pageContext.request.contextPath}/products">Xem tất cả gian hàng</a>
        </div>
    </c:if>

    <div class="shop-layout">
        <form class="filter-panel" method="get" action="${pageContext.request.contextPath}/products">
            <h3>Lọc sản phẩm</h3>
            <div class="field">
                <label for="product-query">Từ khóa</label>
                <input id="product-query" type="search" name="q" value="<c:out value='${query}'/>" placeholder="Tên sản phẩm...">
            </div>
            <div class="field">
                <label for="category">Danh mục</label>
                <select id="category" name="category">
                    <option value="">Tất cả danh mục</option>
                    <c:forEach var="item" items="${categories}">
                        <option value="<c:out value='${item}'/>" ${category == item ? 'selected' : ''}>
                            <c:out value="${item}"/>
                        </option>
                    </c:forEach>
                </select>
            </div>
            <div class="field">
                <span>Khoảng giá</span>
                <div class="price-fields">
                    <input type="number" name="minPrice" value="<c:out value='${minPrice}'/>" min="0" placeholder="Từ">
                    <input type="number" name="maxPrice" value="<c:out value='${maxPrice}'/>" min="0" placeholder="Đến">
                </div>
            </div>
            <div class="field">
                <label for="sort">Sắp xếp</label>
                <select id="sort" name="sort">
                    <option value="">Phổ biến nhất</option>
                    <option value="rating" ${sort == 'rating' ? 'selected' : ''}>Đánh giá cao</option>
                    <option value="priceAsc" ${sort == 'priceAsc' ? 'selected' : ''}>Giá thấp đến cao</option>
                    <option value="priceDesc" ${sort == 'priceDesc' ? 'selected' : ''}>Giá cao đến thấp</option>
                </select>
            </div>
            <c:if test="${not empty storeId}">
                <input type="hidden" name="storeId" value="<c:out value='${storeId}'/>">
            </c:if>
            <div class="button-row">
                <button class="button-primary" type="submit">Áp dụng</button>
                <c:url value="/products" var="clearFiltersUrl">
                    <c:if test="${not empty storeId}"><c:param name="storeId" value="${storeId}"/></c:if>
                </c:url>
                <a class="button-secondary" href="${clearFiltersUrl}">Xóa bộ lọc</a>
            </div>
        </form>

        <section>
            <div class="results-head">
                <strong><c:out value="${totalItems}"/> sản phẩm phù hợp</strong>
                <span class="muted"><c:out value="${category}"/></span>
            </div>
            <c:choose>
                <c:when test="${empty products}">
                    <div class="empty-state">Không tìm thấy sản phẩm phù hợp. Hãy thử từ khóa hoặc mức giá khác.</div>
                </c:when>
                <c:otherwise>
                    <div class="products-grid">
                        <c:forEach var="product" items="${products}">
                            <article class="product-card">
                                <a href="${pageContext.request.contextPath}/product-detail?id=${product.id}">
                                    <div class="product-visual ${product.visualClass}">
                                        <c:if test="${not empty product.tag}">
                                            <span class="product-tag"><c:out value="${product.tag}"/></span>
                                        </c:if>
                                        <c:out value="${product.name}"/>
                                    </div>
                                </a>
                                <div class="product-copy">
                                    <h3><a href="${pageContext.request.contextPath}/product-detail?id=${product.id}"><c:out value="${product.name}"/></a></h3>
                                    <div class="meta"><c:out value="${product.printMethod}"/> · Từ <c:out value="${product.minimumQuantity}"/> sản phẩm</div>
                                    <div class="rating" aria-label="${product.rating} trên 5 sao">
                                        <c:out value="${product.ratingStars}"/><span class="star-off"><c:out value="${product.ratingEmptyStars}"/></span>
                                        <span class="muted">(<c:out value="${product.reviewCount}"/>)</span>
                                    </div>
                                    <div class="product-bottom">
                                        <span class="product-price"><fmt:formatNumber value="${product.price}" type="number" groupingUsed="true"/>đ</span>
                                        <c:choose>
                                            <c:when test="${product.available}">
                                                <form class="inline-form js-quick-cart-form" method="post" action="${pageContext.request.contextPath}/cart">
                                                    <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                                    <input type="hidden" name="action" value="add">
                                                    <input type="hidden" name="shopProductId" value="${product.primaryOffer.shopProductId}">
                                                    <input type="hidden" name="productId" value="${product.id}">
                                                    <input type="hidden" name="quantity" value="${product.minimumQuantity}">
                                                    <button class="add-btn" type="submit"
                                                            title="Thêm ${product.minimumQuantity} sản phẩm vào giỏ hàng"
                                                            aria-label="Thêm <c:out value='${product.name}'/> vào giỏ hàng">+</button>
                                                </form>
                                            </c:when>
                                            <c:otherwise>
                                                <button class="add-btn" type="button" disabled title="Sản phẩm tạm hết hàng" aria-label="Sản phẩm tạm hết hàng">+</button>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                </div>
                            </article>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>

            <%-- Phân trang: giữ nguyên bộ lọc hiện tại qua các tham số query --%>
            <c:if test="${totalPages > 1}">
                <div class="pagination-row" style="display:flex; gap:10px; margin-top:24px; align-items:center;">
                    <c:if test="${currentPage > 1}">
                        <c:url value="/products" var="prevUrl">
                            <c:param name="q" value="${query}"/>
                            <c:param name="category" value="${category}"/>
                            <c:param name="minPrice" value="${minPrice}"/>
                            <c:param name="maxPrice" value="${maxPrice}"/>
                            <c:if test="${not empty storeId}"><c:param name="storeId" value="${storeId}"/></c:if>
                            <c:param name="sort" value="${sort}"/>
                            <c:param name="page" value="${currentPage - 1}"/>
                        </c:url>
                        <a class="button-secondary" href="${prevUrl}">‹ Trang trước</a>
                    </c:if>
                    <span class="muted">Trang <c:out value="${currentPage}"/> / <c:out value="${totalPages}"/></span>
                    <c:if test="${currentPage < totalPages}">
                        <c:url value="/products" var="nextUrl">
                            <c:param name="q" value="${query}"/>
                            <c:param name="category" value="${category}"/>
                            <c:param name="minPrice" value="${minPrice}"/>
                            <c:param name="maxPrice" value="${maxPrice}"/>
                            <c:if test="${not empty storeId}"><c:param name="storeId" value="${storeId}"/></c:if>
                            <c:param name="sort" value="${sort}"/>
                            <c:param name="page" value="${currentPage + 1}"/>
                        </c:url>
                        <a class="button-secondary" href="${nextUrl}">Trang sau ›</a>
                    </c:if>
                </div>
            </c:if>
        </section>
    </div>
</main>
<%@ include file="footer.jsp" %>
<script defer src="${pageContext.request.contextPath}/js/quick-cart.js?v=20261024-1"></script>
</body>
</html>
