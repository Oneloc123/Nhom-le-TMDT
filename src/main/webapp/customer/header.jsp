<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_header.css">
<header>
    <a href="#" class="logo" onclick="switchTab('home');return false;"><span class="mark"></span> PrintKraft</a>
    <nav id="mainNav">
        <a href="#home"        data-tab="home">Trang chủ</a>
        <a href="#san-pham"    data-tab="san-pham">Sản phẩm</a>
        <a href="#studio"      data-tab="studio">Tự thiết kế</a>
        <a href="#doanh-nghiep" data-tab="doanh-nghiep">Doanh nghiệp</a>
        <a href="#nhacungcap"  data-tab="nhacungcap">Nhà cung cấp</a>
        <a href="#donhang"     data-tab="donhang">Đơn hàng</a>
        <a href="#danhgia"     data-tab="danhgia">Đánh giá</a>
        <a href="#faq"         data-tab="faq">Hỗ trợ</a>
    </nav>
    <div class="search-wrap">
        <input type="text" placeholder="Tìm áo thun, cốc sứ, poster...">
        <button>Tìm</button>
    </div>
    <div class="head-actions">
        <div id="authArea">
            <a href="#" onclick="openLogin();return false;" style="font-weight:600;">Tài khoản</a>
        </div>
        <button class="cart-btn" onclick="openCart()">🛒 Giỏ hàng <span class="badge" id="cartBadge">3</span></button>
    </div>
</header>