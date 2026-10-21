<%-- Điều hướng trung tâm tài khoản khách (fragment include).
     KHÔNG khai báo contentType ở đây: trang chứa đã khai báo — JSP cấm
     nhiều page directive contentType khác nhau trong cùng trang.
     Biến activeNav: overview|profile|addresses|orders|favorites|designs|security --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<nav class="acc-nav" aria-label="Điều hướng tài khoản">
    <a class="${activeNav == 'overview' ? 'active' : ''}"
       href="${pageContext.request.contextPath}/account">Tổng quan</a>
    <a class="${activeNav == 'profile' ? 'active' : ''}"
       href="${pageContext.request.contextPath}/profile">Hồ sơ cá nhân</a>
    <a class="${activeNav == 'addresses' ? 'active' : ''}"
       href="${pageContext.request.contextPath}/addresses">Địa chỉ</a>
    <a class="${activeNav == 'orders' ? 'active' : ''}"
       href="${pageContext.request.contextPath}/purchase-history">Đơn hàng</a>
    <a class="${activeNav == 'favorites' ? 'active' : ''}"
       href="${pageContext.request.contextPath}/favorites">Yêu thích</a>
    <a class="${activeNav == 'designs' ? 'active' : ''}"
       href="${pageContext.request.contextPath}/saved-designs">Thiết kế 3D đã lưu</a>
    <a class="${activeNav == 'security' ? 'active' : ''}"
       href="${pageContext.request.contextPath}/profile#security">Bảo mật</a>
</nav>
