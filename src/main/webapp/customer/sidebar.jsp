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
<c:set var="isMyDesigns" value="${rawPath eq '/my-designs' or rawPath eq '/design-request' or fn:contains(rawPath, 'my-designs') or fn:contains(rawPath, 'design-request') or fn:contains(rawPath, 'design-list.jsp')}" />
<c:set var="isDesign" value="${(rawPath eq '/design' or fn:endsWith(rawPath, '/design') or fn:contains(rawPath, '/design?') or fn:contains(rawPath, '/customer/design.jsp')) and not isMyDesigns}" />
<c:set var="isQuotes" value="${rawPath eq '/quotes' or fn:contains(rawPath, 'quotes')}" />
<c:set var="isFavorites" value="${rawPath eq '/favorites' or fn:contains(rawPath, 'favorites')}" />
<c:set var="isMessages" value="${rawPath eq '/messages' or fn:contains(rawPath, 'messages')}" />
<c:set var="isSupport" value="${rawPath eq '/support' or fn:contains(rawPath, 'support')}" />
<c:set var="isNotifications" value="${rawPath eq '/notifications' or fn:contains(rawPath, 'notifications')}" />
<c:set var="isCart" value="${rawPath eq '/cart' or fn:contains(rawPath, 'cart')}" />

<aside class="account-nav-card account-shared-nav"
       aria-label="Điều hướng tài khoản">

    <div class="account-nav-title">Tài khoản PrintKraft</div>

    <a class="account-nav-link ${isAccount ? 'is-current is-active active' : ''}"
       aria-current="${isAccount ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/account">
        <span class="nav-symbol" aria-hidden="true">⌂</span>
        Tổng quan
    </a>

    <a class="account-nav-link ${isProfile ? 'is-current is-active active' : ''}"
       aria-current="${isProfile ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/profile">
        <span class="nav-symbol" aria-hidden="true">◉</span>
        Hồ sơ
    </a>

    <a class="account-nav-link ${isAddresses ? 'is-current is-active active' : ''}"
       aria-current="${isAddresses ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/addresses">
        <span class="nav-symbol" aria-hidden="true">⌖</span>
        Sổ địa chỉ
    </a>

    <a class="account-nav-link ${isOrders ? 'is-current is-active active' : ''}"
       aria-current="${isOrders ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/purchase-history">
        <span class="nav-symbol" aria-hidden="true">▤</span>
        Đơn hàng
    </a>

    <a class="account-nav-link ${isMyDesigns ? 'is-current is-active active' : ''}"
       aria-current="${isMyDesigns ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/my-designs">
        <span class="nav-symbol" aria-hidden="true">◇</span>
        Yêu cầu in 3D
    </a>

    <a class="account-nav-link ${isDesign ? 'is-current is-active active' : ''}"
       aria-current="${isDesign ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/design">
        <span class="nav-symbol" aria-hidden="true">🎨</span>
        Thiết kế 3D
    </a>

    <a class="account-nav-link ${isQuotes ? 'is-current is-active active' : ''}"
       aria-current="${isQuotes ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/quotes">
        <span class="nav-symbol" aria-hidden="true">₫</span>
        Yêu cầu báo giá
    </a>

    <a class="account-nav-link ${isFavorites ? 'is-current is-active active' : ''}"
       aria-current="${isFavorites ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/favorites">
        <span class="nav-symbol" aria-hidden="true">♡</span>
        Yêu thích
    </a>

    <a class="account-nav-link ${isMessages ? 'is-current is-active active' : ''}"
       aria-current="${isMessages ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/messages">
        <span class="nav-symbol" aria-hidden="true">✉</span>
        Tin nhắn xưởng
        <c:if test="${unreadMessages gt 0}">
            <span class="nav-count" aria-label="Tin nhắn chưa đọc">
                <c:out value="${unreadMessages}" />
            </span>
        </c:if>
    </a>

    <a class="account-nav-link ${isSupport ? 'is-current is-active active' : ''}"
       aria-current="${isSupport ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/support">
        <span class="nav-symbol" aria-hidden="true">?</span>
        Hỗ trợ
    </a>

    <a class="account-nav-link ${isNotifications ? 'is-current is-active active' : ''}"
       aria-current="${isNotifications ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/notifications">
        <span class="nav-symbol" aria-hidden="true">♧</span>
        Thông báo
        <c:if test="${unreadNotifications gt 0}">
            <span class="nav-count" aria-label="Thông báo chưa đọc">
                <c:out value="${unreadNotifications}" />
            </span>
        </c:if>
    </a>

    <a class="account-nav-link ${isCart ? 'is-current is-active active' : ''}"
       aria-current="${isCart ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/cart">
        <span class="nav-symbol" aria-hidden="true">🛒</span>
        Giỏ hàng
    </a>

    <a class="account-nav-link"
       href="${pageContext.request.contextPath}/profile#security-settings">
        <span class="nav-symbol" aria-hidden="true">⌑</span>
        Bảo mật
    </a>

</aside>

<script>
(() => {
    try {
        const path = window.location.pathname.replace(/\/+$/, '');
        const sidebar = document.currentScript ? document.currentScript.previousElementSibling : document.querySelector('.account-shared-nav, .account-nav-card');
        if (!sidebar) return;
        const links = Array.from(sidebar.querySelectorAll('a[href]'));

        if (window.location.hash === '#security-settings') {
            const secLink = sidebar.querySelector('a[href*="#security-settings"]');
            if (secLink) {
                links.forEach(l => l.classList.remove('is-active', 'is-current', 'active'));
                secLink.classList.add('is-active', 'is-current', 'active');
                secLink.setAttribute('aria-current', 'page');
                return;
            }
        }

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