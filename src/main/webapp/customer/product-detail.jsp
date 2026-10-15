<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${product.name} - PrintKraft</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css">
</head>
<body>
<%@ include file="header.jsp" %>

<main class="shop-page">
    <div class="detail-layout">
        <div class="detail-visual product-visual ${product.visualClass}">
            <span><c:out value="${product.name}"/></span>
        </div>
        <section class="detail-copy">
            <div class="eyebrow-line"><c:out value="${product.category}"/></div>
            <h1><c:out value="${product.name}"/></h1>
            <div class="rating">★★★★★ <span class="muted"><c:out value="${product.rating}"/>/5 · <c:out value="${product.reviewCount}"/> đánh giá</span></div>
            <p class="description"><c:out value="${product.description}"/></p>
            <div class="detail-price"><fmt:formatNumber value="${product.price}" type="number" groupingUsed="true"/>đ</div>
            <ul class="spec-list">
                <li><span>Phương pháp in</span><b><c:out value="${product.printMethod}"/></b></li>
                <li><span>Số lượng tối thiểu</span><b><c:out value="${product.minimumQuantity}"/> sản phẩm</b></li>
                <li><span>Gian hàng</span><b><c:out value="${product.storeName}"/></b></li>
            </ul>
            <form class="button-row" method="post" action="${pageContext.request.contextPath}/cart">
                <input type="hidden" name="action" value="add">
                <input type="hidden" name="productId" value="${product.id}">
                <label class="field" style="margin:0; width:100px;">
                    <span>Số lượng</span>
                    <input class="quantity-input" type="number" name="quantity" min="${product.minimumQuantity}" value="${product.minimumQuantity}">
                </label>
                <button class="button-primary" type="submit">Thêm vào giỏ hàng</button>
                <a class="button-secondary" href="${pageContext.request.contextPath}/products">Tiếp tục mua</a>
            </form>
        </section>
    </div>
</main>
<%@ include file="footer.jsp" %>
</body>
</html>
