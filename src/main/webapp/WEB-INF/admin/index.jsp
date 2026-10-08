<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="theme-color" content="#f6f5f2">
    <title>Print3D Studio · Admin</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=Space+Grotesk:wght@500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/admin-ui/assets/admin.css">
</head>
<body>
<div id="admin-app" data-context-path="<%= request.getContextPath() %>">
    <div class="boot-screen" role="status" aria-label="Đang tải không gian quản trị">
        <div class="boot-skeleton" aria-hidden="true"><span></span><span></span><span></span></div>
        <p>Đang tải không gian quản trị…</p>
    </div>
</div>
<noscript>Admin UI cần JavaScript để hiển thị các trang quản trị.</noscript>
<script type="module" src="<%= request.getContextPath() %>/admin-ui/js/app.js"></script>
</body>
</html>
