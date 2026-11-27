<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="csrf-token" content="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
    <title>Khu vực xưởng · PrintKraft</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/workshop-ui/workshop.css">
    <script src="https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/three@0.128.0/examples/js/controls/OrbitControls.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/three@0.128.0/examples/js/loaders/OBJLoader.js"></script>
</head>
<body>
<div id="workshop-app" data-context-path="<%= request.getContextPath() %>">
    <main class="boot-state" role="status">Đang tải dữ liệu xưởng…</main>
</div>
<div id="workshop-toast" aria-live="polite"></div>
<script type="module" src="<%= request.getContextPath() %>/workshop-ui/workshop.js"></script>
</body>
</html>
