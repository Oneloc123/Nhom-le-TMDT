<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="csrf-token" content="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
    <title>Khu vực xưởng · PrintKraft</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/workshop-ui/workshop.css">
</head>
<body>
<div id="workshop-app" data-context-path="<%= request.getContextPath() %>">
    <main class="boot-state" role="status">Đang tải dữ liệu xưởng…</main>
</div>
<div id="workshop-toast" aria-live="polite"></div>
<script type="module" src="<%= request.getContextPath() %>/workshop-ui/workshop.js"></script>
</body>
</html>
