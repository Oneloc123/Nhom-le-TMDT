<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sản phẩm yêu thích | PrintKraft</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/user-v2.css?v=20261104-account-layout">
</head>
<body>
<%@ include file="header.jsp" %>
<main class="uv2-page uv2-favorites-page">
    <div class="uv2-breadcrumb"><a href="${pageContext.request.contextPath}/home">Trang chủ</a><span>/</span><a href="${pageContext.request.contextPath}/account">Tài khoản</a><span>/</span><span>Sản phẩm yêu thích</span></div>
    <div class="uv2-page-heading">
        <div><p class="uv2-eyebrow">BỘ SƯU TẬP CỦA BẠN</p><h1>Sản phẩm yêu thích</h1><p>Những sản phẩm bạn đã lưu để xem lại hoặc đặt in sau này.</p></div>
        <a class="uv2-button is-light" href="${pageContext.request.contextPath}/products">Khám phá sản phẩm</a>
    </div>
    <div class="uv2-account-grid">
        <jsp:include page="/WEB-INF/views/user/account-sidebar.jsp"/>
        <div class="uv2-content">
            <section class="uv2-panel uv2-favorites-panel">
                <div class="uv2-panel-head"><div><p class="uv2-eyebrow">ĐÃ LƯU</p><h2>Danh sách yêu thích</h2></div><span class="uv2-result-count"><c:out value="${fn:length(products)}"/> sản phẩm</span></div>
                <c:choose>
                    <c:when test="${empty products}">
                        <div class="uv2-empty"><div class="uv2-empty-icon" aria-hidden="true">♡</div><b>Bạn chưa lưu sản phẩm nào</b><p>Nhấn biểu tượng yêu thích trên sản phẩm để lưu những mẫu bạn quan tâm.</p><a class="uv2-button is-primary" href="${pageContext.request.contextPath}/products">Xem sản phẩm</a></div>
                    </c:when>
                    <c:otherwise>
                        <div class="products-grid uv2-products-grid">
                            <c:forEach var="product" items="${products}">
                                <article class="product-card uv2-favorite-product-card">
                                    <a class="uv2-favorite-product-link" href="${pageContext.request.contextPath}/product-detail?id=${product.id}">
                                        <div class="product-visual ${product.visualClass}"><c:out value="${product.name}"/></div>
                                    </a>
                                    <div class="product-copy">
                                        <h3><a href="${pageContext.request.contextPath}/product-detail?id=${product.id}"><c:out value="${product.name}"/></a></h3>
                                        <div class="meta"><c:out value="${product.printMethod}"/> · Từ <c:out value="${product.minimumQuantity}"/> sản phẩm</div>
                                        <div class="product-bottom">
                                            <span class="product-price"><fmt:formatNumber value="${product.price}" type="number" groupingUsed="true"/>đ</span>
                                            <form class="inline-form" method="post" action="${pageContext.request.contextPath}/favorites">
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
            </section>
        </div>
    </div>
</main>
<%@ include file="footer.jsp" %>
</body>
</html>