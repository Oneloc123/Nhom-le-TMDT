<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Hồ sơ — PrintKraft 3D</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth.css">
</head>
<body class="auth-body">

<div class="profile-shell">
    <header class="profile-topbar">
        <a class="brand" href="${pageContext.request.contextPath}/"><span class="cube"></span> PrintKraft <b>3D</b></a>
        <nav>
            <a href="${pageContext.request.contextPath}/customer/home.jsp">Trang chủ</a>
            <a class="active" href="${pageContext.request.contextPath}/profile">Hồ sơ</a>
            <a href="${pageContext.request.contextPath}/logout">Đăng xuất</a>
        </nav>
    </header>

    <main class="profile-main">
        <div class="page-head">
            <div>
                <h1>Hồ sơ cá nhân</h1>
                <p>Quản lý thông tin liên hệ và địa chỉ nhận hàng in 3D của bạn.</p>
            </div>
            <span class="badge ${user.role == 'ADMIN' ? 'badge-admin' : 'badge-customer'}">
                <c:out value="${user.role}"/>
            </span>
        </div>

        <c:if test="${not empty message}">
            <div class="alert alert-ok"><c:out value="${message}"/></div>
        </c:if>
        <c:if test="${not empty error}">
            <div class="alert alert-err"><c:out value="${error}"/></div>
        </c:if>

        <div class="profile-grid">
            <%-- Cập nhật thông tin --%>
            <section class="panel">
                <div class="panel-head"><h2>Thông tin cá nhân</h2></div>
                <form method="post" action="${pageContext.request.contextPath}/profile" class="stack-form">
                    <input type="hidden" name="action" value="info">
                    <label>Họ và tên
                        <input type="text" name="fullName" value="<c:out value='${user.fullName}'/>" required>
                    </label>
                    <label>Email
                        <input type="email" value="<c:out value='${user.email}'/>" disabled>
                    </label>
                    <label>Số điện thoại
                        <input type="text" name="phone" value="<c:out value='${user.phone}'/>" placeholder="09xx xxx xxx">
                    </label>
                    <label>Địa chỉ nhận hàng in 3D
                        <input type="text" name="address" value="<c:out value='${user.address}'/>"
                               placeholder="12 Nguyễn Huệ, Q.1, TP.HCM">
                    </label>
                    <button type="submit" class="btn-primary btn-block">Lưu thay đổi</button>
                </form>
            </section>

            <div class="side-panels">
                <%-- Đổi mật khẩu --%>
                <section class="panel">
                    <div class="panel-head"><h2>Đổi mật khẩu</h2></div>
                    <form method="post" action="${pageContext.request.contextPath}/profile" class="stack-form">
                        <input type="hidden" name="action" value="password">
                        <label>Mật khẩu hiện tại
                            <input type="password" name="oldPassword" required>
                        </label>
                        <label>Mật khẩu mới
                            <input type="password" name="newPassword" placeholder="Tối thiểu 6 ký tự" required>
                        </label>
                        <label>Xác nhận mật khẩu mới
                            <input type="password" name="confirmPassword" required>
                        </label>
                        <button type="submit" class="btn-primary btn-block">Đổi mật khẩu</button>
                    </form>
                </section>

                <%-- Thông tin tài khoản --%>
                <section class="panel">
                    <div class="panel-head"><h2>Tài khoản</h2></div>
                    <ul class="info-list">
                        <li><span>Vai trò</span><b><c:out value="${user.role}"/></b></li>
                        <li><span>Trạng thái</span><b>
                            <c:choose>
                                <c:when test="${user.status == 'ACTIVE'}">Hoạt động</c:when>
                                <c:when test="${user.status == 'LOCKED'}">Bị khóa</c:when>
                                <c:otherwise>Chờ kích hoạt</c:otherwise>
                            </c:choose>
                        </b></li>
                        <li><span>Đăng nhập qua</span><b><c:out value="${user.provider}"/></b></li>
                        <li><span>Ngày tạo</span><b><c:out value="${user.createdAt}"/></b></li>
                    </ul>
                </section>
            </div>
        </div>
    </main>
</div>

</body>
</html>
