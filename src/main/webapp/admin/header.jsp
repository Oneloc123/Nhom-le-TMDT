<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin.css">

<header class="admin-topbar">
    <a class="admin-logo" href="${pageContext.request.contextPath}/admin/dashboard">
        <span class="cube"></span> PrintKraft <b>3D</b>
    </a>

    <div class="topbar-search">
        <input type="text" placeholder="Tìm đơn in, file .stl / .obj, khách hàng...">
        <button type="button">Tìm</button>
    </div>

    <div class="topbar-actions">
        <span class="chip">🔔 <b>3</b> đơn mới</span>
        <div class="user-chip">
            <span class="avatar"><c:out value="${fn:substring(currentUser.fullName, 0, 1)}"/></span>
            <div class="user-meta">
                <b><c:out value="${currentUser.fullName}"/></b>
                <small>Quản trị viên</small>
            </div>
            <a class="logout" href="${pageContext.request.contextPath}/logout" title="Đăng xuất">⏻</a>
        </div>
    </div>
</header>
