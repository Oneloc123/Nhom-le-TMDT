<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quản lý người dùng — PrintKraft 3D</title>
</head>
<body class="admin-body">

<jsp:include page="header.jsp"/>

<div class="admin-layout">
    <jsp:include page="sidebar.jsp">
        <jsp:param name="page" value="users"/>
    </jsp:include>

    <main class="admin-main">
        <div class="page-head">
            <div>
                <h1>Quản lý người dùng</h1>
                <p>Xem, thêm, sửa và khóa tài khoản Admin / Customer.</p>
            </div>
        </div>

        <c:if test="${not empty message}">
            <div class="alert alert-ok"><c:out value="${message}"/></div>
        </c:if>
        <c:if test="${not empty error}">
            <div class="alert alert-err"><c:out value="${error}"/></div>
        </c:if>

        <!-- Thẻ tổng hợp -->
        <div class="stat-grid">
            <div class="stat-card blue">
                <div class="stat-ico">👥</div>
                <div class="stat-body">
                    <span class="stat-label">Tổng người dùng</span>
                    <b class="stat-value"><fmt:formatNumber value="${totalUsers}"/></b>
                </div>
            </div>
            <div class="stat-card violet">
                <div class="stat-ico">🛡️</div>
                <div class="stat-body">
                    <span class="stat-label">Quản trị viên</span>
                    <b class="stat-value"><fmt:formatNumber value="${adminCount}"/></b>
                </div>
            </div>
            <div class="stat-card green">
                <div class="stat-ico">🛍️</div>
                <div class="stat-body">
                    <span class="stat-label">Khách hàng</span>
                    <b class="stat-value"><fmt:formatNumber value="${customerCount}"/></b>
                </div>
            </div>
            <div class="stat-card orange">
                <div class="stat-ico">🔒</div>
                <div class="stat-body">
                    <span class="stat-label">Đang khóa</span>
                    <b class="stat-value"><fmt:formatNumber value="${lockedCount}"/></b>
                </div>
            </div>
        </div>

        <div class="panel-grid">
            <!-- Danh sách người dùng -->
            <section class="panel panel-wide">
                <div class="panel-head">
                    <h2>Danh sách người dùng</h2>
                </div>
                <table class="data-table">
                    <thead>
                    <tr>
                        <th>ID</th>
                        <th>Họ tên</th>
                        <th>Email</th>
                        <th>SĐT</th>
                        <th>Địa chỉ nhận bản in</th>
                        <th>Vai trò</th>
                        <th>Trạng thái</th>
                        <th>Nguồn</th>
                        <th>Thao tác</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="u" items="${users}">
                        <tr>
                            <td><b>#<c:out value="${u.id}"/></b></td>
                            <td><c:out value="${u.fullName}"/></td>
                            <td><c:out value="${u.email}"/></td>
                            <td><c:out value="${u.phone}"/></td>
                            <td class="addr-cell"><c:out value="${u.address}"/></td>
                            <td>
                                <c:choose>
                                    <c:when test="${u.role == 'ADMIN'}"><span class="badge badge-admin">Admin</span></c:when>
                                    <c:otherwise><span class="badge badge-customer">Customer</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <c:choose>
                                    <c:when test="${u.status == 'ACTIVE'}"><span class="badge badge-done">Hoạt động</span></c:when>
                                    <c:when test="${u.status == 'LOCKED'}"><span class="badge badge-locked">Khóa</span></c:when>
                                    <c:otherwise><span class="badge badge-new">Chờ kích hoạt</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td><c:out value="${u.provider}"/></td>
                            <td class="actions">
                                <a class="btn-mini btn-edit"
                                   href="${pageContext.request.contextPath}/admin/users?action=edit&id=${u.id}">Sửa</a>
                                <c:choose>
                                    <c:when test="${u.status == 'LOCKED'}">
                                        <a class="btn-mini btn-unlock"
                                           href="${pageContext.request.contextPath}/admin/users?action=toggle&id=${u.id}">Mở khóa</a>
                                    </c:when>
                                    <c:otherwise>
                                        <a class="btn-mini btn-lock"
                                           href="${pageContext.request.contextPath}/admin/users?action=toggle&id=${u.id}">Khóa</a>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty users}">
                        <tr><td colspan="9" class="empty">Chưa có người dùng nào.</td></tr>
                    </c:if>
                    </tbody>
                </table>
            </section>

            <!-- Form thêm / sửa -->
            <div class="side-panels">
                <section class="panel">
                    <div class="panel-head">
                        <h2><c:choose>
                            <c:when test="${not empty editUser}">Sửa người dùng</c:when>
                            <c:otherwise>Thêm người dùng</c:otherwise>
                        </c:choose></h2>
                    </div>

                    <c:choose>
                        <c:when test="${not empty editUser}">
                            <form method="post" action="${pageContext.request.contextPath}/admin/users" class="stack-form">
                                <input type="hidden" name="action" value="edit">
                                <input type="hidden" name="id" value="${editUser.id}">
                                <label>Email
                                    <input type="text" value="${editUser.email}" disabled>
                                </label>
                                <label>Họ tên
                                    <input type="text" name="fullName" value="${editUser.fullName}" required>
                                </label>
                                <label>Số điện thoại
                                    <input type="text" name="phone" value="${editUser.phone}">
                                </label>
                                <label>Địa chỉ nhận bản in
                                    <input type="text" name="address" value="${editUser.address}">
                                </label>
                                <label>Vai trò
                                    <select name="role">
                                        <option value="CUSTOMER" ${editUser.role == 'CUSTOMER' ? 'selected' : ''}>Customer</option>
                                        <option value="ADMIN" ${editUser.role == 'ADMIN' ? 'selected' : ''}>Admin</option>
                                    </select>
                                </label>
                                <button type="submit" class="btn-primary btn-block">Lưu thay đổi</button>
                                <a class="btn-ghost btn-block" href="${pageContext.request.contextPath}/admin/users">Hủy / quay lại</a>
                            </form>
                        </c:when>
                        <c:otherwise>
                            <form method="post" action="${pageContext.request.contextPath}/admin/users" class="stack-form">
                                <input type="hidden" name="action" value="add">
                                <label>Email
                                    <input type="email" name="email" placeholder="vidu@gmail.com" required>
                                </label>
                                <label>Mật khẩu
                                    <input type="text" name="password" placeholder="Tối thiểu 6 ký tự" required>
                                </label>
                                <label>Họ tên
                                    <input type="text" name="fullName" placeholder="Nguyễn Văn A" required>
                                </label>
                                <label>Số điện thoại
                                    <input type="text" name="phone" placeholder="09xx xxx xxx">
                                </label>
                                <label>Địa chỉ nhận bản in
                                    <input type="text" name="address" placeholder="12 Nguyễn Huệ, Q.1, TP.HCM">
                                </label>
                                <label>Vai trò
                                    <select name="role">
                                        <option value="CUSTOMER">Customer</option>
                                        <option value="ADMIN">Admin</option>
                                    </select>
                                </label>
                                <button type="submit" class="btn-primary btn-block">➕ Thêm người dùng</button>
                            </form>
                        </c:otherwise>
                    </c:choose>
                </section>
            </div>
        </div>
    </main>
</div>

</body>
</html>
