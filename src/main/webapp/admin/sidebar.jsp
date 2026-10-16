<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<aside class="admin-sidebar">
    <nav>
        <div class="nav-label">Tổng quan</div>
        <a class="${param.page == 'dashboard' ? 'active' : ''}"
           href="${pageContext.request.contextPath}/admin/dashboard">
            <span class="ico">▦</span> Dashboard
        </a>

        <div class="nav-label">Quản trị hệ thống</div>
        <a class="${param.page == 'users' ? 'active' : ''}"
           href="${pageContext.request.contextPath}/admin/users">
            Người dùng
        </a>
        <a href="#">Đơn đặt in 3D</a>
        <a href="#">File thiết kế (.stl/.obj)</a>
        <a href="#">Công nghệ in (FDM/SLA)</a>
        <a href="#">Vật liệu (PLA/ABS/Resin)</a>

        <div class="nav-label">Báo cáo</div>
        <a href="#">Doanh thu</a>
        <a href="#">Hiệu suất máy in</a>

        <div class="sidebar-card">
            <div class="layers">▟</div>
            <b>Máy in đang rảnh</b>
            <p>2/3 máy FDM · 1/2 máy SLA sẵn sàng nhận đơn.</p>
        </div>
    </nav>
</aside>
