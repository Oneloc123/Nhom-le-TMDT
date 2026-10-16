<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="vn.edu.hcmuaf.fit.tmdt_nhomle.service.CartService" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_header.css">
<header>        <a href="${pageContext.request.contextPath}/home" class="logo"><span class="mark"></span> PrintKraft</a>
        <nav id="mainNav">
            <a href="${pageContext.request.contextPath}/home">Trang chủ</a>
        <a href="${pageContext.request.contextPath}/products">Sản phẩm</a>
        <a href="${pageContext.request.contextPath}/products?category=Quà+tặng+doanh+nghiệp">Doanh nghiệp</a>            <a href="${pageContext.request.contextPath}/stores">Nhà cung cấp</a>
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
