<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sản phẩm yêu thích - PrintKraft</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css">
</head>
<body>
<%@ include file="header.jsp" %>

<main class="shop-page">
    <div class="shop-heading">
        <div>
            <div class="eyebrow-line">Tài khoản</div>
            <h1>Sản phẩm yêu thích</h1>
            <p>Những sản phẩm bạn đã lưu để xem lại sau.</p>
        </div>
        <a class="button-secondary" href="${pageContext.request.contextPath}/products">Xem sản phẩm</a>
    </div>

    <c:choose>
        <c:when test="${empty products}">
            <div class="empty-state">Bạn chưa lưu sản phẩm nào.
            <a class="button-secondary" style="margin-left:10px;"
               href="${pageContext.request.contextPath}/products">Khám phá sản phẩm</a>
        </div>
        </c:when>
        <c:otherwise>
            <div class="products-grid">
                <c:forEach var="product" items="${products}">
                    <article class="product-card">
                        <a href="${pageContext.request.contextPath}/product-detail?id=${product.id}">
                            <div class="product-visual ${product.visualClass}">
                                <c:out value="${product.name}"/>
                            </div>
                        </a>
                        <div class="product-copy">
                            <h3><a href="${pageContext.request.contextPath}/product-detail?id=${product.id}"><c:out value="${product.name}"/></a></h3>
                            <div class="meta"><c:out value="${product.printMethod}"/> · Từ <c:out value="${product.minimumQuantity}"/> sản phẩm</div>
                            <div class="product-bottom">
                                <span class="product-price"><fmt:formatNumber value="${product.price}" type="number" groupingUsed="true"/>đ</span>
                                <form class="inline-form" method="post" data-pk-fav
                                      action="${pageContext.request.contextPath}/favorites">
                                    <input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
                                    <input type="hidden" name="action" value="remove">
                                    <input type="hidden" name="productId" value="${product.id}">
                                    <button class="button-danger" type="submit" title="Bỏ khỏi yêu thích">Bỏ lưu</button>
                                </form>
                            </div>
                        </div>
                    </article>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</main>
<%@ include file="footer.jsp" %>
<script src="${pageContext.request.contextPath}/js/account.js"></script>
</body>
</html>
