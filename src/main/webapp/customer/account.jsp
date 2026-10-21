<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Tài khoản — PrintKraft 3D</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
</head>
<body>
<%@ include file="header.jsp" %>

<main class="shop-page acc-page">
    <div class="shop-heading">
        <div>
            <div class="eyebrow-line">Tài khoản</div>
            <h1>Xin chào, <c:out value="${empty user.fullName ? user.email : user.fullName}"/></h1>
            <p>Quản lý hồ sơ, địa chỉ, đơn hàng, yêu thích và thiết kế 3D của bạn.</p>
        </div>
        <span class="acc-badge ${user.role == 'ADMIN' ? 'acc-badge-admin' : 'acc-badge-user'}">
            <c:out value="${user.role}"/>
        </span>
    </div>

    <%@ include file="account-nav.jsp" %>

    <c:if test="${not empty error}">
        <div class="notice-err"><c:out value="${error}"/></div>
    </c:if>

    <%-- ============ Thẻ thống kê (số liệu thật từ DB) ============ --%>
    <section class="acc-stats" aria-label="Tổng quan số liệu">
        <article class="acc-stat">
            <span class="acc-stat-label">Đơn hàng</span>
            <strong class="acc-stat-value"><c:out value="${overview.orderCount}"/></strong>
            <a href="${pageContext.request.contextPath}/purchase-history">Xem lịch sử &raquo;</a>
        </article>
        <article class="acc-stat">
            <span class="acc-stat-label">Sản phẩm yêu thích</span>
            <strong class="acc-stat-value"><c:out value="${overview.favoriteCount}"/></strong>
            <a href="${pageContext.request.contextPath}/favorites">Xem danh sách &raquo;</a>
        </article>
        <article class="acc-stat">
            <span class="acc-stat-label">Thiết kế 3D đã lưu</span>
            <strong class="acc-stat-value"><c:out value="${designCount}"/></strong>
            <a href="${pageContext.request.contextPath}/saved-designs">Mở thư viện &raquo;</a>
        </article>
        <article class="acc-stat">
            <span class="acc-stat-label">Địa chỉ nhận hàng</span>
            <strong class="acc-stat-value"><c:out value="${overview.addressCount}"/></strong>
            <a href="${pageContext.request.contextPath}/addresses">Quản lý &raquo;</a>
        </article>
    </section>

    <div class="acc-grid">
        <%-- ============ Hồ sơ & nhanh tắt ============ --%>
        <section class="acc-card">
            <div class="acc-card-head">
                <h2>Hồ sơ của bạn</h2>
                <a class="acc-link" href="${pageContext.request.contextPath}/profile">Sửa hồ sơ</a>
            </div>
            <ul class="acc-info">
                <li><span>Họ và tên</span><b><c:out value="${empty user.fullName ? '—' : user.fullName}"/></b></li>
                <li><span>Email</span><b><c:out value="${user.email}"/></b></li>
                <li><span>Số điện thoại</span><b><c:out value="${empty user.phone ? '—' : user.phone}"/></b></li>
                <li><span>Xác thực email</span><b>
                    <c:choose>
                        <c:when test="${user.emailVerified}"><span class="acc-ok">Đã xác thực</span></c:when>
                        <c:otherwise><span class="acc-warn">Chưa xác thực</span></c:otherwise>
                    </c:choose>
                </b></li>
                <li><span>Trạng thái</span><b>
                    <c:choose>
                        <c:when test="${user.status == 'ACTIVE'}">Hoạt động</c:when>
                        <c:when test="${user.status == 'LOCKED'}">Bị khóa</c:when>
                        <c:otherwise>Chờ kích hoạt</c:otherwise>
                    </c:choose>
                </b></li>
                <li><span>Tham gia</span><b><c:out value="${user.createdAt}"/></b></li>
            </ul>
            <div class="acc-actions">
                <a class="acc-btn" href="${pageContext.request.contextPath}/addresses">Thêm địa chỉ</a>
                <a class="acc-btn acc-btn-ghost" href="${pageContext.request.contextPath}/profile#security">Đổi mật khẩu</a>
            </div>
        </section>

        <%-- ============ Đơn hàng gần đây ============ --%>
        <section class="acc-card">
            <div class="acc-card-head">
                <h2>Đơn hàng gần đây</h2>
                <a class="acc-link" href="${pageContext.request.contextPath}/purchase-history">Tất cả đơn</a>
            </div>
            <c:choose>
                <c:when test="${empty overview.recentOrders}">
                    <div class="acc-empty">
                        Bạn chưa có đơn hàng nào.
                        <a href="${pageContext.request.contextPath}/products">Khám phá sản phẩm &raquo;</a>
                    </div>
                </c:when>
                <c:otherwise>
                    <ul class="acc-orders">
                        <c:forEach var="order" items="${overview.recentOrders}">
                            <li>
                                <div>
                                    <b><c:out value="${order.orderCode}"/></b>
                                    <span class="acc-muted"><c:out value="${order.placedAt}"/></span>
                                </div>
                                <div class="acc-order-mid">
                                    <span class="acc-status acc-status-${order.status}"><c:out value="${order.status}"/></span>
                                    <b><fmt:formatNumber value="${order.totalAmount}" type="number" groupingUsed="true"/>đ</b>
                                </div>
                                <a class="acc-link"
                                   href="${pageContext.request.contextPath}/order-detail?code=${order.orderCode}">Chi tiết</a>
                            </li>
                        </c:forEach>
                    </ul>
                </c:otherwise>
            </c:choose>
        </section>
    </div>

    <%-- ============ Trạng thái đơn (nếu có) ============ --%>
    <c:if test="${not empty overview.orderStatusCounts}">
        <section class="acc-card acc-card-wide">
            <div class="acc-card-head"><h2>Đơn hàng theo trạng thái</h2></div>
            <div class="acc-status-row">
                <c:forEach var="row" items="${overview.orderStatusCounts}">
                    <span class="acc-chip">
                        <c:out value="${row.status}"/>
                        <b><c:out value="${row.count}"/></b>
                    </span>
                </c:forEach>
            </div>
        </section>
    </c:if>
</main>

<%@ include file="footer.jsp" %>
</body>
</html>
