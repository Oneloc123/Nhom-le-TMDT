<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="theme-color" content="#F5F7FB">
    <meta name="csrf-token" content="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
    <title>PrintKraft · Admin</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=Space+Grotesk:wght@500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/admin-ui/assets/admin.css?v=finance-pages-20261126-r1">
</head>
<body>
<div id="admin-app" data-context-path="<%= request.getContextPath() %>">
    <div class="boot-screen" role="status" aria-label="Đang tải không gian quản trị">
        <div class="boot-skeleton" aria-hidden="true"><span></span><span></span><span></span></div>
        <p>Đang tải không gian quản trị…</p>
    </div>
</div>
<noscript>Admin UI cần JavaScript để hiển thị các trang quản trị.</noscript>
<script>
(function () {
    var root = document.getElementById("admin-app");
    window.setTimeout(function () {
        if (!root || !root.querySelector(".boot-screen")) return;
        root.innerHTML = '<section class="error-state" role="alert"><div class="eyebrow">ADMIN KHỞI ĐỘNG THẤT BẠI</div><h1>Không thể mở giao diện quản trị</h1><p>Các tệp JavaScript chưa tải xong sau 15 giây. Hãy tải lại bằng Ctrl + F5 và kiểm tra tab Console nếu lỗi vẫn còn.</p><button class="button" type="button" onclick="window.location.reload()">Tải lại trang</button></section>';
    }, 15000);
})();
</script>
<script type="module" src="<%= request.getContextPath() %>/admin-ui/js/app.js?v=admin-loading-timeout-20261126-r1"></script>
</body>
</html>
