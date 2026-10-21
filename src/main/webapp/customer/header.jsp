<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="vn.edu.hcmuaf.fit.tmdt_nhomle.service.CartService" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_header.css">
<header>
    <a href="${pageContext.request.contextPath}/home" class="logo"><span class="mark"></span> PrintKraft</a>
    <nav id="mainNav">
        <a href="${pageContext.request.contextPath}/home">Trang chủ</a>
        <a href="${pageContext.request.contextPath}/products">Sản phẩm</a>
        <a href="${pageContext.request.contextPath}/design">Thiết kế 3D</a>
        <a href="${pageContext.request.contextPath}/products?category=Quà+tặng+doanh+nghiệp">Doanh nghiệp</a>
        <a href="${pageContext.request.contextPath}/stores">Nhà cung cấp</a>
        <a href="${pageContext.request.contextPath}/quotes">Báo giá</a>
        <a href="${pageContext.request.contextPath}/favorites">Yêu thích</a>
        <a href="${pageContext.request.contextPath}/purchase-history">Đơn hàng</a>
    </nav>
    <form class="search-wrap" method="get" action="${pageContext.request.contextPath}/products">
        <input type="search" name="q" placeholder="Tìm áo thun, cốc sứ, poster..." aria-label="Tìm sản phẩm">
        <button type="submit">Tìm</button>
    </form>
    <div class="head-actions">
        <div id="authArea">
            <c:choose>
                <c:when test="${not empty sessionScope.currentUser}">
                    <a href="${pageContext.request.contextPath}/profile">Tài khoản</a>
                </c:when>
                <c:otherwise>
                    <a href="${pageContext.request.contextPath}/login">Đăng nhập</a>
                </c:otherwise>
            </c:choose>
        </div>
        <a class="cart-btn" href="${pageContext.request.contextPath}/cart">
            Giỏ hàng <span class="badge" id="cartBadge">
                <%= new CartService().totalQuantity(request) %>
            </span>
        </a>
    </div>
</header>
<script>
    (function () {
        const path = window.location.pathname;
        const search = decodeURIComponent(window.location.search || '');
        const links = document.querySelectorAll('#mainNav a');

        let bestMatch = null;
        let bestScore = -1;

        links.forEach(a => {
            a.classList.remove('active');
            const href = a.getAttribute('href');
            if (!href) return;

            const [linkPath, linkQuery] = href.split('?');
            const decodedLinkQuery = linkQuery ? decodeURIComponent(linkQuery) : '';

            const isExactPath = (path === linkPath);
            const isSubPath = (!decodedLinkQuery && linkPath.endsWith('/products') && path.endsWith('/product-detail'))
                || (linkPath.endsWith('/stores') && path.endsWith('/store-detail'))
                || (linkPath.endsWith('/purchase-history') && path.endsWith('/order-detail'))
                || (linkPath.endsWith('/home') && (path.endsWith('/') || path.endsWith('/home')));

            if (!isExactPath && !isSubPath) return;

            let score = 0;
            if (decodedLinkQuery) {
                if (search.includes(decodedLinkQuery) || search.includes(decodedLinkQuery.replace(/\+/g, ' '))) {
                    score = 10;
                } else {
                    score = -1;
                }
            } else {
                if (search.includes('Quà tặng doanh nghiệp') || search.includes('Quà+tặng+doanh+nghiệp')) {
                    score = -1;
                } else {
                    score = 1;
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestMatch = a;
            }
        });

        if (bestMatch) {
            bestMatch.classList.add('active');
        }
    })();
</script>
