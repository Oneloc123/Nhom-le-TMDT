<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<c:set var="rawPath" value="${requestScope['jakarta.servlet.forward.servlet_path']}" />
<c:if test="${empty rawPath}">
    <c:set var="rawPath" value="${requestScope['jakarta.servlet.forward.request_uri']}" />
</c:if>
<c:if test="${empty rawPath}">
    <c:set var="rawPath" value="${pageContext.request.servletPath}" />
</c:if>
<c:if test="${empty rawPath}">
    <c:set var="rawPath" value="${pageContext.request.requestURI}" />
</c:if>

<c:set var="isAccount" value="${rawPath eq '/account' or fn:endsWith(rawPath, '/account') or fn:contains(rawPath, '/account?') or fn:contains(rawPath, 'dashboard.jsp')}" />
<c:set var="isProfile" value="${(rawPath eq '/profile' or fn:endsWith(rawPath, '/profile') or fn:contains(rawPath, '/profile?') or fn:contains(rawPath, 'profile.jsp') or fn:contains(rawPath, 'change-password')) and not isAccount}" />
<c:set var="isAddresses" value="${rawPath eq '/addresses' or fn:contains(rawPath, 'addresses')}" />
<c:set var="isOrders" value="${rawPath eq '/purchase-history' or rawPath eq '/order-detail' or fn:contains(rawPath, 'purchase-history') or fn:contains(rawPath, 'order-detail') or fn:contains(rawPath, 'order-tracking') or fn:contains(rawPath, 'order-change-request') or fn:contains(rawPath, 'orders.jsp')}" />
<c:set var="isReturnRefunds" value="${rawPath eq '/return-refunds' or fn:contains(rawPath, 'return-refunds')}" />
<c:set var="isMyDesigns" value="${rawPath eq '/my-designs' or rawPath eq '/design-request' or fn:contains(rawPath, 'my-designs') or fn:contains(rawPath, 'design-request') or fn:contains(rawPath, 'design-list.jsp')}" />
<c:set var="isDesign" value="${(rawPath eq '/design' or fn:endsWith(rawPath, '/design') or fn:contains(rawPath, '/design?') or fn:contains(rawPath, '/customer/design.jsp')) and not isMyDesigns}" />
<c:set var="isFavorites" value="${rawPath eq '/favorites' or fn:contains(rawPath, 'favorites')}" />
<c:set var="isQuotes" value="${rawPath eq '/quotes' or fn:contains(rawPath, 'quotes')}" />
<c:set var="isMessages" value="${rawPath eq '/messages' or fn:contains(rawPath, 'messages')}" />
<c:set var="isNotifications" value="${rawPath eq '/notifications' or fn:contains(rawPath, 'notifications')}" />
<c:set var="isSupport" value="${rawPath eq '/support' or fn:contains(rawPath, 'support')}" />
<c:set var="isCart" value="${rawPath eq '/cart' or fn:contains(rawPath, 'cart')}" />

<aside class="uv2-side" aria-label="Điều hướng tài khoản">
    <b>KHÔNG GIAN CÁ NHÂN</b>

    <a class="${isAccount ? 'is-active is-current active' : ''}"
       aria-current="${isAccount ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/account">
        Tổng quan
    </a>

    <a class="${isProfile ? 'is-active is-current active' : ''}"
       aria-current="${isProfile ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/profile">
        Thông tin cá nhân
    </a>

    <a class="${isAddresses ? 'is-active is-current active' : ''}"
       aria-current="${isAddresses ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/addresses">
        Sổ địa chỉ
    </a>

    <a class="${isOrders ? 'is-active is-current active' : ''}"
       aria-current="${isOrders ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/purchase-history">
        Đơn hàng của tôi
    </a>

    <a class="${isReturnRefunds ? 'is-active is-current active' : ''}"
       aria-current="${isReturnRefunds ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/return-refunds">
        Đổi trả &amp; hoàn tiền
    </a>

    <a class="${isMyDesigns ? 'is-active is-current active' : ''}"
       aria-current="${isMyDesigns ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/my-designs">
        Yêu cầu in 3D
    </a>

    <a class="${isDesign ? 'is-active is-current active' : ''}"
       aria-current="${isDesign ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/design">
        Thiết kế 3D
    </a>

    <a class="${isFavorites ? 'is-active is-current active' : ''}"
       aria-current="${isFavorites ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/favorites">
        Sản phẩm yêu thích
    </a>

    <a class="${isQuotes ? 'is-active is-current active' : ''}"
       aria-current="${isQuotes ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/quotes">
        Yêu cầu báo giá
    </a>

    <a class="${isMessages ? 'is-active is-current active' : ''}"
       aria-current="${isMessages ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/messages">
        Tin nhắn xưởng
        <c:if test="${unreadMessages gt 0}">
            <span class="uv2-nav-count">
                <c:out value="${unreadMessages}"/>
            </span>
        </c:if>
    </a>

    <a class="${isNotifications ? 'is-active is-current active' : ''}"
       aria-current="${isNotifications ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/notifications">
        Thông báo
        <c:if test="${unreadNotifications gt 0}">
            <span class="uv2-nav-count">
                <c:out value="${unreadNotifications}"/>
            </span>
        </c:if>
    </a>

    <a class="${isSupport ? 'is-active is-current active' : ''}"
       aria-current="${isSupport ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/support">
        Hỗ trợ &amp; khiếu nại
    </a>

    <a class="${isCart ? 'is-active is-current active' : ''}"
       aria-current="${isCart ? 'page' : 'false'}"
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

<script>
(() => {
    try {
        const path = window.location.pathname.replace(/\/+$/, '');
        const sidebar = document.currentScript ? document.currentScript.previousElementSibling : document.querySelector('.uv2-side');
        if (!sidebar) return;
        const links = Array.from(sidebar.querySelectorAll('a[href]'));
        const alreadyActive = links.some(a => a.classList.contains('is-active') || a.classList.contains('is-current') || a.classList.contains('active'));
        if (!alreadyActive) {
            for (const a of links) {
                const href = a.getAttribute('href') || '';
                const cleanHref = href.split('?')[0].split('#')[0].replace(/\/+$/, '');
                if (cleanHref && (path === cleanHref || path.endsWith(cleanHref))) {
                    a.classList.add('is-active', 'is-current', 'active');
                    a.setAttribute('aria-current', 'page');
                    return;
                }
            }
            for (const a of links) {
                const href = a.getAttribute('href') || '';
                if ((path.includes('order-detail') || path.includes('order-tracking') || path.includes('order-change-request')) && href.includes('purchase-history')) {
                    a.classList.add('is-active', 'is-current', 'active');
                    a.setAttribute('aria-current', 'page');
                    return;
                }
                if (path.includes('design-request') && href.includes('my-designs')) {
                    a.classList.add('is-active', 'is-current', 'active');
                    a.setAttribute('aria-current', 'page');
                    return;
                }
            }
        }
    } catch (e) {}
})();
</script>
<script defer src="${pageContext.request.contextPath}/js/account-sidebar-spa.js?v=20261126-customer-sync-r1"></script>