<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Không tìm thấy - PrintKraft</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cus_home.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shop.css">
</head>
<body>
<%@ include file="header.jsp" %>
<main class="shop-page">
    <div class="empty-state" style="padding:60px 20px;">
        <h1 style="font-size:28px;">404 — Không tìm thấy</h1>
        <p style="margin:14px 0;"><c:out value="${empty message ? 'Nội dung bạn tìm không tồn tại.' : message}"/></p>
        <a class="button-primary" href="${pageContext.request.contextPath}/home">Về trang chủ</a>
        <a class="button-secondary" href="${pageContext.request.contextPath}/products" style="margin-left:10px;">Xem sản phẩm</a>
    </div>
</main>
<%@ include file="footer.jsp" %>
</body>
</html>
