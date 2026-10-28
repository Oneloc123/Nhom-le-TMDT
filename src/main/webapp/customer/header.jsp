<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="vn.edu.hcmuaf.fit.tmdt_nhomle.service.CartService" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_header.css?v=20261025-1">

<header class="site-header" id="siteHeader">
    <a href="${pageContext.request.contextPath}/home" class="logo" aria-label="PrintKraft - Trang chủ">
        <span class="mark" aria-hidden="true"></span> PrintKraft
    </a>
    <button class="nav-toggle" id="navToggle" type="button" aria-expanded="false"
            aria-controls="mainNav" aria-label="Mở trình đơn">
        <span></span><span></span><span></span>
    </button>
    <nav id="mainNav" aria-label="Điều hướng chính">
        <a href="${pageContext.request.contextPath}/home">Trang chủ</a>
        <a href="${pageContext.request.contextPath}/products">Sản phẩm</a>
        <a href="${pageContext.request.contextPath}/design">Thiết kế 3D</a>
        <a href="${pageContext.request.contextPath}/products?category=Quà+tặng+doanh+nghiệp">Doanh nghiệp</a>
        <a href="${pageContext.request.contextPath}/stores">Nhà cung cấp</a>
        <a href="${pageContext.request.contextPath}/quotes">Báo giá</a>
        <a href="${pageContext.request.contextPath}/favorites">Yêu thích</a>
        <a href="${pageContext.request.contextPath}/purchase-history">Đơn hàng</a>
    </nav>
    <form class="search-wrap" method="get" action="${pageContext.request.contextPath}/products" role="search">
        <label class="visually-hidden" for="headerProductSearch">Tìm sản phẩm</label>
        <input id="headerProductSearch" type="search" name="q" placeholder="Tìm áo thun, cốc sứ, poster...">
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
            <span aria-hidden="true">🛒</span> <span class="cart-label">Giỏ hàng</span>
            <span class="badge" id="cartBadge" aria-live="polite" aria-label="Số lượng sản phẩm trong giỏ">
                <%= new CartService().totalQuantity(request) %>
            </span>
        </a>
    </div>
</header>
<script defer src="${pageContext.request.contextPath}/js/header.js?v=20261025-1"></script>
