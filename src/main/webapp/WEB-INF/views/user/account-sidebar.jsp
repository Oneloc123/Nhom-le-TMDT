<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:set var="accountActivePath"
       value="${requestScope['jakarta.servlet.forward.servlet_path']}" />

<c:if test="${empty accountActivePath}">
    <c:set var="accountActivePath"
           value="${pageContext.request.servletPath}" />
</c:if>

<aside class="uv2-side" aria-label="Điều hướng tài khoản">
    <b>KHÔNG GIAN CÁ NHÂN</b>

    <a class="${accountActivePath eq '/account' ? 'is-active' : ''}"
       aria-current="${accountActivePath eq '/account' ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/account">
        Tổng quan
    </a>

    <a class="${accountActivePath eq '/profile' ? 'is-active' : ''}"
       aria-current="${accountActivePath eq '/profile' ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/profile">
        Thông tin cá nhân
    </a>

    <a class="${accountActivePath eq '/addresses' ? 'is-active' : ''}"
       aria-current="${accountActivePath eq '/addresses' ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/addresses">
        Sổ địa chỉ
    </a>

    <a class="${accountActivePath eq '/purchase-history' or accountActivePath eq '/order-detail' ? 'is-active' : ''}"
       aria-current="${accountActivePath eq '/purchase-history' or accountActivePath eq '/order-detail' ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/purchase-history">
        Đơn hàng của tôi
    </a>

    <a class="${accountActivePath eq '/my-designs' ? 'is-active' : ''}"
       aria-current="${accountActivePath eq '/my-designs' ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/my-designs">
        Yêu cầu in 3D
    </a>

    <a class="${accountActivePath eq '/design' ? 'is-active' : ''}"
       aria-current="${accountActivePath eq '/design' ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/design">
        Thiết kế 3D
    </a>

    <a class="${accountActivePath eq '/favorites' ? 'is-active' : ''}"
       aria-current="${accountActivePath eq '/favorites' ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/favorites">
        Sản phẩm yêu thích
    </a>

    <a class="${accountActivePath eq '/messages' ? 'is-active' : ''}"
       aria-current="${accountActivePath eq '/messages' ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/messages">
        Tin nhắn xưởng
        <c:if test="${unreadMessages gt 0}">
            <span class="uv2-nav-count">
                <c:out value="${unreadMessages}"/>
            </span>
        </c:if>
    </a>

    <a class="${accountActivePath eq '/cart' ? 'is-active' : ''}"
       aria-current="${accountActivePath eq '/cart' ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/cart">
        Giỏ hàng
    </a>

    <form method="post" action="${pageContext.request.contextPath}/logout">
        <input type="hidden"
               name="csrfToken"
               value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">

        <button class="uv2-side-logout" type="submit">
            Đăng xuất
        </button>
    </form>
</aside>