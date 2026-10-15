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
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css">
</head>
<body>
<%@ include file="header.jsp" %>

<main class="shop-page">
    <div class="shop-heading">
        <div>
            <div class="eyebrow-line">Sản phẩm &amp; mua hàng</div>
            <h1>Chọn sản phẩm phù hợp</h1>
            <p>Khám phá các sản phẩm in theo yêu cầu từ những gian hàng được đánh giá cao.</p>
        </div>
        <a class="button-secondary" href="${pageContext.request.contextPath}/stores">Tìm gian hàng</a>
    </div>

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
                <a class="button-secondary" href="${pageContext.request.contextPath}/products">Xóa lọc</a>
            </div>
        </form>

        <section>
            <div class="results-head">
                <strong><c:out value="${fn:length(products)}"/> sản phẩm phù hợp</strong>
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
                                    <div class="rating">★★★★★ <span class="muted">(<c:out value="${product.reviewCount}"/>)</span></div>
                                    <div class="product-bottom">
                                        <span class="product-price"><fmt:formatNumber value="${product.price}" type="number" groupingUsed="true"/>đ</span>
                                        <form class="inline-form" method="post" action="${pageContext.request.contextPath}/cart">
                                            <input type="hidden" name="action" value="add">
                                            <input type="hidden" name="productId" value="${product.id}">
                                            <input type="hidden" name="quantity" value="1">
                                            <button class="add-btn" type="submit" title="Thêm vào giỏ hàng">+</button>
                                        </form>
                                    </div>
                                </div>
                            </article>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>
    </div>
</main>
<%@ include file="footer.jsp" %>
</body>
</html>
