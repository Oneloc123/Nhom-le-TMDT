<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="vn.edu.hcmuaf.fit.tmdt_nhomle.service.CartService" %>
<%@ page import="vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_header.css?v=20261125-modern1">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/mini-cart.css?v=20261110-1">
<header class="site-header" id="siteHeader">
    <a href="${pageContext.request.contextPath}/home" class="logo" aria-label="PrintKraft - Trang chủ">
        <span class="mark" aria-hidden="true"></span> PrintKraft
    </a>
    <button class="nav-toggle" id="navToggle" type="button" aria-expanded="false"
            aria-controls="mainNav" aria-label="Mở trình đơn">
        <span></span><span></span><span></span>
    </button>
    <c:set var="ctx" value="${pageContext.request.contextPath}" />
    <c:set var="fwdServlet" value="${requestScope['jakarta.servlet.forward.servlet_path']}" />
    <c:set var="fwdUri" value="${requestScope['jakarta.servlet.forward.request_uri']}" />
    <c:set var="srvPath" value="${pageContext.request.servletPath}" />
    <c:set var="reqUri" value="${pageContext.request.requestURI}" />
    <c:set var="allPaths" value=" ${fwdServlet} ${fwdUri} ${srvPath} ${reqUri} " />

    <c:set var="currCategory" value="${param.category}" />
    <c:set var="isHdrEnterprise" value="${fn:contains(currCategory, 'doanh') or fn:contains(currCategory, 'Doanh')}" />
    <c:set var="isHdrProducts" value="${(fn:contains(allPaths, '/products') or fn:contains(allPaths, 'products.jsp') or fn:contains(allPaths, 'product-detail')) and not isHdrEnterprise}" />
    <c:set var="isHdrDesign" value="${(fn:contains(allPaths, '/design') or fn:contains(allPaths, 'design.jsp')) and not fn:contains(allPaths, 'my-designs')}" />
    <c:set var="isHdrStores" value="${fn:contains(allPaths, '/stores') or fn:contains(allPaths, '/store-detail') or fn:contains(allPaths, 'stores.jsp') or fn:contains(allPaths, 'store-detail.jsp')}" />
    <c:set var="isHdrQuotes" value="${fn:contains(allPaths, '/quotes') or fn:contains(allPaths, 'quotes.jsp')}" />
    <c:set var="isHdrFavorites" value="${fn:contains(allPaths, '/favorites') or fn:contains(allPaths, 'favorites.jsp')}" />
    <c:set var="isHdrOrders" value="${fn:contains(allPaths, 'purchase-history') or fn:contains(allPaths, 'order-detail') or fn:contains(allPaths, 'order-tracking') or fn:contains(allPaths, 'order-change-request') or fn:contains(allPaths, 'orders.jsp')}" />
    <c:set var="isHdrHome" value="${not isHdrEnterprise and not isHdrProducts and not isHdrDesign and not isHdrStores and not isHdrQuotes and not isHdrFavorites and not isHdrOrders and (fn:contains(allPaths, 'home') or fn:contains(allPaths, 'index.jsp') or fwdUri eq ctx or fwdUri eq '${ctx}/' or reqUri eq ctx or reqUri eq '${ctx}/' or srvPath eq '/customer/home.jsp' or fwdServlet eq '/home' or fwdServlet eq '/')}" />

    <nav id="mainNav" aria-label="Điều hướng chính">
        <a class="${isHdrHome ? 'is-active active' : ''}" aria-current="${isHdrHome ? 'page' : 'false'}" href="${pageContext.request.contextPath}/home">Trang chủ</a>
        <a class="${isHdrProducts ? 'is-active active' : ''}" aria-current="${isHdrProducts ? 'page' : 'false'}" href="${pageContext.request.contextPath}/products">Sản phẩm</a>
        <a class="${isHdrDesign ? 'is-active active' : ''}" aria-current="${isHdrDesign ? 'page' : 'false'}" href="${pageContext.request.contextPath}/design">Thiết kế 3D</a>
        <a class="${isHdrEnterprise ? 'is-active active' : ''}" aria-current="${isHdrEnterprise ? 'page' : 'false'}" href="${pageContext.request.contextPath}/products?category=Quà+tặng+doanh+nghiệp">Doanh nghiệp</a>
        <a class="${isHdrStores ? 'is-active active' : ''}" aria-current="${isHdrStores ? 'page' : 'false'}" href="${pageContext.request.contextPath}/stores">Nhà cung cấp</a>
        <a class="${isHdrQuotes ? 'is-active active' : ''}" aria-current="${isHdrQuotes ? 'page' : 'false'}" href="${pageContext.request.contextPath}/quotes">Báo giá</a>
        <a class="${isHdrFavorites ? 'is-active active' : ''}" aria-current="${isHdrFavorites ? 'page' : 'false'}" href="${pageContext.request.contextPath}/favorites">Yêu thích</a>
        <a class="${isHdrOrders ? 'is-active active' : ''}" aria-current="${isHdrOrders ? 'page' : 'false'}" href="${pageContext.request.contextPath}/purchase-history">Đơn hàng</a>
    </nav>
    <form class="search-wrap" method="get" action="${pageContext.request.contextPath}/products" role="search">
        <label class="visually-hidden" for="headerProductSearch">Tìm sản phẩm</label>
        <input id="headerProductSearch" type="search" name="q" placeholder="Tìm áo thun, cốc sứ, poster...">
        <button type="submit">Tìm</button>
    </form>

    <div class="head-actions">
        <div id="authArea" class="auth-area">
            <c:choose>
                <c:when test="${not empty sessionScope.currentUser}">
                    <c:set var="userName"   value="${empty sessionScope.currentUser.fullName ? 'Tài khoản' : sessionScope.currentUser.fullName}" />
                    <c:set var="userEmail"  value="${sessionScope.currentUser.email}" />
                    <c:set var="userInitial" value="${empty sessionScope.currentUser.fullName ? 'U' : fn:toUpperCase(fn:substring(sessionScope.currentUser.fullName, 0, 1))}" />

                    <%-- Chuông thông báo --%>
                    <details class="header-notification-menu">
                        <summary aria-label="Mở thông báo">
                            <span class="bell-icon" aria-hidden="true">
                                <svg width="19" height="19" viewBox="0 0 24 24" fill="none"
                                     stroke="currentColor" stroke-width="2"
                                     stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
                                    <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
                                </svg>
                            </span>
                            <c:if test="${unreadNotifications gt 0}">
                                <span class="notif-count"><c:out value="${unreadNotifications}" /></span>
                            </c:if>
                        </summary>
                        <div class="header-notification-panel">
                            <div class="header-notification-title">
                                <b>Thông báo mới</b>
                                <a href="${pageContext.request.contextPath}/notifications">Xem tất cả</a>
                            </div>
                            <c:choose>
                                <c:when test="${empty recentNotifications}">
                                    <p class="header-notification-empty">Chưa có thông báo.</p>
                                </c:when>
                                <c:otherwise>
                                    <c:forEach var="notice" items="${recentNotifications}">
                                        <a class="header-notification-item" href="${pageContext.request.contextPath}/notifications">
                                            <b><c:out value="${notice.title}" /></b>
                                            <span><c:out value="${notice.message}" /></span>
                                            <small><c:out value="${notice.createdAt}" /></small>
                                        </a>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </details>

                    <%-- Menu tài khoản có avatar --%>
                    <details class="account-menu">
                        <summary class="account-btn" aria-label="Mở menu tài khoản">
                            <span class="account-avatar" aria-hidden="true"><c:out value="${userInitial}" /></span>
                            <span class="account-name"><c:out value="${userName}" /></span>
                            <span class="account-caret" aria-hidden="true">
                                <svg width="12" height="12" viewBox="0 0 24 24" fill="none"
                                     stroke="currentColor" stroke-width="2.5"
                                     stroke-linecap="round" stroke-linejoin="round">
                                    <polyline points="6 9 12 15 18 9"></polyline>
                                </svg>
                            </span>
                        </summary>
                        <div class="account-panel">
                            <div class="account-panel-head">
                                <span class="account-avatar account-avatar-lg" aria-hidden="true"><c:out value="${userInitial}" /></span>
                                <div class="account-info">
                                    <strong><c:out value="${userName}" /></strong>
                                    <c:if test="${not empty userEmail}">
                                        <small><c:out value="${userEmail}" /></small>
                                    </c:if>
                                </div>
                            </div>
                            <nav class="account-links" aria-label="Menu tài khoản">
                                <a href="${pageContext.request.contextPath}/profile">
                                    <span class="mi" aria-hidden="true">👤</span> Trang cá nhân
                                </a>
                                <a href="${pageContext.request.contextPath}/purchase-history">
                                    <span class="mi" aria-hidden="true">📦</span> Đơn hàng của tôi
                                </a>
                                <a href="${pageContext.request.contextPath}/favorites">
                                    <span class="mi" aria-hidden="true">❤️</span> Sản phẩm yêu thích
                                </a>
                                <a href="${pageContext.request.contextPath}/quotes">
                                    <span class="mi" aria-hidden="true">📝</span> Yêu cầu báo giá
                                </a>
                                <a href="${pageContext.request.contextPath}/notifications">
                                    <span class="mi" aria-hidden="true">🔔</span> Thông báo
                                    <c:if test="${unreadNotifications gt 0}">
                                        <span class="account-badge"><c:out value="${unreadNotifications}" /></span>
                                    </c:if>
                                </a>
                            </nav>
                            <div class="account-sep"></div>
                            <form method="post" action="${pageContext.request.contextPath}/logout" class="account-logout">
                                <input type="hidden" name="csrfToken" value="<%= Csrf.ensureToken(request) %>">
                                <button type="submit">
                                    <span aria-hidden="true">↪</span> Đăng xuất
                                </button>
                            </form>
                        </div>
                    </details>
                </c:when>
                <c:otherwise>
                    <a href="${pageContext.request.contextPath}/login" class="login-btn">
                        <span aria-hidden="true">👤</span> Đăng nhập
                    </a>
                </c:otherwise>
            </c:choose>
        </div>

        <div class="mini-cart-wrap">
            <button class="cart-btn mini-cart-toggle" id="miniCartToggle" type="button"
                    data-context-path="${pageContext.request.contextPath}"
                    aria-controls="miniCartPanel" aria-expanded="false" aria-label="Mở giỏ hàng mini">
                <span aria-hidden="true">🛒</span> <span class="cart-label">Giỏ hàng</span>
                <span class="badge" id="cartBadge" aria-live="polite" aria-label="Số lượng sản phẩm trong giỏ">
                    <%= new CartService().totalQuantity(request) %>
                </span>
            </button>
            <section class="mini-cart-panel" id="miniCartPanel" hidden aria-label="Giỏ hàng xem nhanh">
                <div class="mini-cart-header">
                    <div>
                        <strong>Giỏ hàng của bạn</strong>
                        <small id="miniCartCountText">Xem nhanh sản phẩm đã chọn</small>
                    </div>
                    <button class="mini-cart-close" id="miniCartClose" type="button" aria-label="Đóng giỏ hàng mini">×</button>
                </div>
                <div class="mini-cart-items" id="miniCartItems" aria-live="polite">
                    <div class="mini-cart-loading">Mở giỏ hàng để xem sản phẩm.</div>
                </div>
                <div class="mini-cart-footer">
                    <div class="mini-cart-total"><span>Tạm tính</span><strong id="miniCartSubtotal">0đ</strong></div>
                    <div class="mini-cart-actions">
                        <a class="mini-cart-link is-secondary" href="${pageContext.request.contextPath}/cart">Xem giỏ hàng</a>
                        <a class="mini-cart-link is-primary" href="${pageContext.request.contextPath}/order">Đặt hàng</a>
                    </div>
                </div>
            </section>
        </div>
    </div>
</header>
<jsp:include page="/customer/chatbot-widget.jsp"/>
<script defer src="${pageContext.request.contextPath}/js/header.js?v=20261112-v3"></script>
<script defer src="${pageContext.request.contextPath}/js/mini-cart.js?v=20261110-1"></script>