<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:set var="navPath"
       value="${requestScope['jakarta.servlet.forward.servlet_path']}" />

<c:if test="${empty navPath}">
    <c:set var="navPath" value="${pageContext.request.servletPath}" />
</c:if>

<aside class="account-nav-card account-shared-nav"
       aria-label="Điều hướng tài khoản">

    <div class="account-nav-title">Tài khoản PrintKraft</div>

    <a class="account-nav-link${navPath eq '/account' ? ' is-current' : ''}"
       aria-current="${navPath eq '/account' ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/account">
        <span class="nav-symbol" aria-hidden="true">⌂</span>
        Tổng quan
    </a>

    <a class="account-nav-link${navPath eq '/profile' ? ' is-current' : ''}"
       aria-current="${navPath eq '/profile' ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/profile">
        <span class="nav-symbol" aria-hidden="true">◉</span>
        Hồ sơ
    </a>

    <a class="account-nav-link${navPath eq '/addresses' ? ' is-current' : ''}"
       aria-current="${navPath eq '/addresses' ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/addresses">
        <span class="nav-symbol" aria-hidden="true">⌖</span>
        Sổ địa chỉ
    </a>

    <a class="account-nav-link${navPath eq '/purchase-history' ? ' is-current' : ''}"
       aria-current="${navPath eq '/purchase-history' ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/purchase-history">
        <span class="nav-symbol" aria-hidden="true">▤</span>
        Đơn hàng
    </a>

    <a class="account-nav-link${navPath eq '/quotes' ? ' is-current' : ''}"
       aria-current="${navPath eq '/quotes' ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/quotes">
        <span class="nav-symbol" aria-hidden="true">₫</span>
        Yêu cầu báo giá
    </a>

    <a class="account-nav-link${navPath eq '/favorites' ? ' is-current' : ''}"
       aria-current="${navPath eq '/favorites' ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/favorites">
        <span class="nav-symbol" aria-hidden="true">♡</span>
        Yêu thích
    </a>

    <a class="account-nav-link${navPath eq '/messages' ? ' is-current' : ''}"
       aria-current="${navPath eq '/messages' ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/messages">
        <span class="nav-symbol" aria-hidden="true">✉</span>
        Tin nhắn xưởng
        <c:if test="${unreadMessages gt 0}">
            <span class="nav-count" aria-label="Tin nhắn chưa đọc">
                <c:out value="${unreadMessages}" />
            </span>
        </c:if>
    </a>

    <a class="account-nav-link${navPath eq '/support' ? ' is-current' : ''}"
       aria-current="${navPath eq '/support' ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/support">
        <span class="nav-symbol" aria-hidden="true">?</span>
        Hỗ trợ
    </a>

    <a class="account-nav-link${navPath eq '/notifications' ? ' is-current' : ''}"
       aria-current="${navPath eq '/notifications' ? 'page' : 'false'}"
       href="${pageContext.request.contextPath}/notifications">
        <span class="nav-symbol" aria-hidden="true">♧</span>
        Thông báo
        <c:if test="${unreadNotifications gt 0}">
            <span class="nav-count" aria-label="Thông báo chưa đọc">
                <c:out value="${unreadNotifications}" />
            </span>
        </c:if>
    </a>

    <a class="account-nav-link"
       href="${pageContext.request.contextPath}/profile#security-settings">
        <span class="nav-symbol" aria-hidden="true">⌑</span>
        Bảo mật
    </a>

</aside>