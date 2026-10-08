<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard — PrintKraft 3D</title>
</head>
<body class="admin-body">

<jsp:include page="header.jsp"/>

<div class="admin-layout">
    <jsp:include page="sidebar.jsp">
        <jsp:param name="page" value="dashboard"/>
    </jsp:include>

    <main class="admin-main">
        <div class="page-head">
            <div>
                <h1>Tổng quan hệ thống in 3D</h1>
                <p>Theo dõi đơn đặt in, doanh thu và người dùng theo thời gian thực.</p>
            </div>
            <a class="btn-primary" href="${pageContext.request.contextPath}/admin/users">➕ Quản lý người dùng</a>
        </div>

        <!-- Thẻ thống kê -->
        <div class="stat-grid">
            <div class="stat-card violet">
                <div class="stat-ico">🖨️</div>
                <div class="stat-body">
                    <span class="stat-label">Tổng đơn đặt in 3D</span>
                    <b class="stat-value"><fmt:formatNumber value="${totalOrders}"/></b>
                    <small class="stat-sub">${newOrders} mới · ${printingOrders} đang in</small>
                </div>
            </div>

            <div class="stat-card green">
                <div class="stat-ico">💰</div>
                <div class="stat-body">
                    <span class="stat-label">Doanh thu</span>
                    <b class="stat-value"><fmt:formatNumber value="${revenueVnd}" type="currency"
                                                            currencySymbol="₫" groupingUsed="true"
                                                            maxFractionDigits="0"/></b>
                    <small class="stat-sub">${doneOrders} hoàn tất · ${shippedOrders} đã giao</small>
                </div>
            </div>

            <div class="stat-card blue">
                <div class="stat-ico">👥</div>
                <div class="stat-body">
                    <span class="stat-label">Tổng người dùng</span>
                    <b class="stat-value"><fmt:formatNumber value="${totalUsers}"/></b>
                    <small class="stat-sub">${customerCount} khách · ${adminCount} quản trị</small>
                </div>
            </div>

            <div class="stat-card orange">
                <div class="stat-ico">🛡️</div>
                <div class="stat-body">
                    <span class="stat-label">Tài khoản cần chú ý</span>
                    <b class="stat-value"><fmt:formatNumber value="${lockedCount + pendingCount}"/></b>
                    <small class="stat-sub">${lockedCount} khóa · ${pendingCount} chờ kích hoạt</small>
                </div>
            </div>
        </div>

        <div class="panel-grid">
            <!-- Đơn in gần đây -->
            <section class="panel">
                <div class="panel-head">
                    <h2>Đơn đặt in gần đây</h2>
                    <a href="#">Xem tất cả →</a>
                </div>
                <table class="data-table">
                    <thead>
                    <tr>
                        <th>Mã đơn</th>
                        <th>Khách hàng</th>
                        <th>File thiết kế</th>
                        <th>Công nghệ</th>
                        <th>Chất liệu</th>
                        <th>Trọng lượng</th>
                        <th>Thời gian in</th>
                        <th>Báo giá</th>
                        <th>Trạng thái</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="o" items="${recentOrders}">
                        <tr>
                            <td><b>#<c:out value="${o.id}"/></b></td>
                            <td><c:out value="${o.customerName}"/></td>
                            <td><span class="file-tag"><c:out value="${o.fileName}"/></span></td>
                            <td>
                                <c:choose>
                                    <c:when test="${o.technology == 'FDM'}"><span class="badge badge-fdm">FDM</span></c:when>
                                    <c:otherwise><span class="badge badge-sla">SLA</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td><c:out value="${o.material}"/></td>
                            <td><fmt:formatNumber value="${o.weightGram}" maxFractionDigits="0"/> g</td>
                            <td><fmt:formatNumber value="${o.printHours}" maxFractionDigits="1"/> giờ</td>
                            <td class="price-cell"><fmt:formatNumber value="${o.priceVnd}" type="currency"
                                                                     currencySymbol="₫" maxFractionDigits="0"/></td>
                            <td>
                                <c:choose>
                                    <c:when test="${o.status == 'Moi'}"><span class="badge badge-new">Mới</span></c:when>
                                    <c:when test="${o.status == 'Dang in'}"><span class="badge badge-printing">Đang in</span></c:when>
                                    <c:when test="${o.status == 'Hoan tat'}"><span class="badge badge-done">Hoàn tất</span></c:when>
                                    <c:otherwise><span class="badge badge-shipped">Đã giao</span></c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty recentOrders}">
                        <tr><td colspan="9" class="empty">Chưa có đơn đặt in nào.</td></tr>
                    </c:if>
                    </tbody>
                </table>
            </section>

            <!-- Biểu đồ theo công nghệ & chất liệu -->
            <div class="side-panels">
                <section class="panel">
                    <div class="panel-head"><h2>Theo công nghệ in</h2></div>
                    <ul class="bar-list">
                        <c:forEach var="entry" items="${ordersByTech}">
                            <li>
                                <span class="bar-label"><c:out value="${entry.key}"/></span>
                                <div class="bar-track">
                                    <div class="bar-fill tech"
                                         style="width:${entry.value * 100 / (totalOrders eq 0 ? 1 : totalOrders)}%"></div>
                                </div>
                                <span class="bar-val"><c:out value="${entry.value}"/> đơn</span>
                            </li>
                        </c:forEach>
                    </ul>
                </section>

                <section class="panel">
                    <div class="panel-head"><h2>Theo chất liệu</h2></div>
                    <ul class="bar-list">
                        <c:forEach var="entry" items="${ordersByMaterial}">
                            <li>
                                <span class="bar-label"><c:out value="${entry.key}"/></span>
                                <div class="bar-track">
                                    <div class="bar-fill mat"
                                         style="width:${entry.value * 100 / (totalOrders eq 0 ? 1 : totalOrders)}%"></div>
                                </div>
                                <span class="bar-val"><c:out value="${entry.value}"/> đơn</span>
                            </li>
                        </c:forEach>
                    </ul>
                </section>
            </div>
        </div>
    </main>
</div>

</body>
</html>
