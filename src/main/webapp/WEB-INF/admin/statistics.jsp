<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="true" %>
<%@ page import="java.time.LocalDate" %>
<%
    String contextPath = request.getContextPath();
    LocalDate statsToday = LocalDate.now();
    LocalDate statsMonthStart = statsToday.withDayOfMonth(1);
    String csrfToken = vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request);
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="theme-color" content="#f7f7fb">
    <meta name="csrf-token" content="<%= csrfToken %>">
    <title>Thống kê & phân tích · PrintKraft Admin</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=Space+Grotesk:wght@500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="<%= contextPath %>/admin-ui/assets/admin.css">
    <style>
        /* Statistics page only. The application frame, navigation and header come from admin.css. */
        .stats-page{width:100%;min-width:0}
        .stats-page .heading-row{display:flex;align-items:flex-end;justify-content:space-between;gap:22px;margin:0 0 24px}
        .stats-page .eyebrow{margin:0 0 7px;color:var(--violet);font-size:9px;font-weight:800;letter-spacing:1.15px}
        .stats-page .page-title{margin:0;color:var(--ink);font:600 clamp(25px,2.1vw,31px)/1.12 "Space Grotesk",sans-serif;letter-spacing:-.85px}
        .stats-page .page-description{max-width:680px;margin:8px 0 0;color:var(--muted);font-size:12px;line-height:1.6}
        .stats-page .heading-actions{display:flex;flex:0 0 auto;gap:8px;align-items:center}
        .stats-page .button{display:inline-flex;min-height:38px;align-items:center;justify-content:center;gap:8px;padding:0 13px;border:1px solid var(--line);border-radius:9px;background:#fff;color:#5f687a;font:600 11px "DM Sans",sans-serif;cursor:pointer;transition:background .16s,border-color .16s,transform .16s}
        .stats-page .button:hover{border-color:#c9c4f3;background:#faf9ff}
        .stats-page .button:active{transform:translateY(1px)}
        .stats-page .button-primary{border-color:var(--violet);background:var(--violet);color:#fff;box-shadow:0 5px 12px #7164d51f}
        .stats-page .button-primary:hover{border-color:var(--violet-dark);background:var(--violet-dark)}
        .stats-page .button:disabled{cursor:wait;opacity:.55}
        .stats-page .filter-panel,.stats-page .panel{min-width:0;border:1px solid var(--line);border-radius:14px;background:var(--paper);box-shadow:0 5px 18px #24213a08}
        .stats-page .filter-panel{margin-bottom:17px;padding:17px 18px}
        .stats-page .filter-top{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:14px}
        .stats-page .section-label{color:var(--ink);font-size:12px;font-weight:750}
        .stats-page .filter-helper{color:var(--muted);font-size:10px}
        .stats-page .filters{display:grid;grid-template-columns:repeat(4,minmax(120px,1fr)) minmax(150px,auto);gap:11px;align-items:end}
        .stats-page .field{display:grid;min-width:0;gap:6px}
        .stats-page .field label{color:#727c90;font-size:10px;font-weight:700}
        .stats-page .field input,.stats-page .field select{width:100%;min-width:0;height:38px;padding:0 10px;border:1px solid #e4e7ef;border-radius:8px;outline:0;background:#fff;color:#354056;font:500 11px "DM Sans",sans-serif}
        .stats-page .field input:focus,.stats-page .field select:focus{border-color:#958be9;box-shadow:0 0 0 3px #7164d515}
        .stats-page .filter-apply{width:100%;white-space:nowrap}
        .stats-page .filter-buttons{display:flex;flex-wrap:wrap;gap:7px;margin-top:13px;padding-top:12px;border-top:1px solid #eff0f5}
        .stats-page .preset-button{min-height:28px;padding:0 10px;border:1px solid #e6e8f0;border-radius:7px;background:#fff;color:#737d90;font:600 10px "DM Sans",sans-serif;cursor:pointer}
        .stats-page .preset-button:hover,.stats-page .preset-button.active{border-color:#c8c2f5;background:#f5f3ff;color:#5e53c7}
        .stats-page .status-message{display:none;margin:0 0 15px;padding:10px 13px;border:1px solid transparent;border-radius:9px;font-size:11px}
        .stats-page .status-message.show{display:block}
        .stats-page .status-message.loading{border-color:#e6e2ff;background:#f5f3ff;color:#6258c3}
        .stats-page .status-message.error{border-color:#f4d6da;background:#fff4f5;color:#a53f4c}
        .stats-page .status-message.success{border-color:#d7eee3;background:#f0faf5;color:#267b5c}
        .stats-page .kpi-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px;margin-bottom:17px}
        .stats-page .kpi-card{position:relative;min-width:0;overflow:hidden;padding:15px 16px;border:1px solid var(--line);border-radius:13px;background:#fff;box-shadow:0 4px 14px #24213a07}
        .stats-page .kpi-head{display:flex;align-items:center;justify-content:space-between;gap:10px;color:#697489;font-size:10px;font-weight:700}
        .stats-page .kpi-icon{display:grid;width:32px;height:32px;flex:0 0 32px;place-items:center;border-radius:9px;background:#f0eeff;color:#6d61d7;font-size:15px}
        .stats-page .kpi-green .kpi-icon{background:#eaf7f1;color:#258461}
        .stats-page .kpi-blue .kpi-icon{background:#edf3ff;color:#4c78c5}
        .stats-page .kpi-amber .kpi-icon{background:#fff5e8;color:#bd812e}
        .stats-page .kpi-rose .kpi-icon{background:#fff0f2;color:#c85c6b}
        .stats-page .kpi-cyan .kpi-icon{background:#eaf8fa;color:#32899a}
        .stats-page .kpi-value{overflow:hidden;margin:12px 0 7px;color:#202a40;font:600 clamp(20px,2vw,26px)/1.15 "Space Grotesk",sans-serif;letter-spacing:-.6px;text-overflow:ellipsis;white-space:nowrap}
        .stats-page .kpi-bottom{display:flex;flex-wrap:wrap;justify-content:space-between;gap:4px 8px;color:#8b94a5;font-size:9px}
        .stats-page .kpi-change{font-weight:750}
        .stats-page .kpi-change.up{color:#21845f}.stats-page .kpi-change.down{color:#c65a69}.stats-page .kpi-change.neutral{color:#7e8798}
        .stats-page .kpi-decoration{position:absolute;right:-36px;bottom:-48px;width:85px;height:85px;border:13px solid #f6f5ff;border-radius:50%;pointer-events:none}
        .stats-page .charts-grid{display:grid;grid-template-columns:minmax(0,1.45fr) minmax(280px,1fr);gap:13px;margin-bottom:13px}
        .stats-page .charts-grid.equal{grid-template-columns:repeat(2,minmax(0,1fr))}
        .stats-page .panel{padding:17px}
        .stats-page .panel-head{display:flex;align-items:flex-start;justify-content:space-between;gap:12px;margin-bottom:13px}
        .stats-page .panel-title{margin:0;color:#283248;font-size:12px;font-weight:750}
        .stats-page .panel-subtitle{margin:5px 0 0;color:#8a93a5;font-size:9px;line-height:1.5}
        .stats-page .panel-tag{flex:0 0 auto;padding:5px 8px;border-radius:6px;background:#f4f4f9;color:#737d90;font-size:9px;font-weight:700;white-space:nowrap}
        .stats-page .bar-viewport{width:100%;overflow-x:auto;overflow-y:hidden;padding-bottom:4px}
        .stats-page .bar-viewport svg{display:block;max-width:none}
        .stats-page .chart-empty{display:grid;min-height:205px;place-items:center;padding:18px;border:1px dashed #e3e6ee;border-radius:10px;color:#8993a5;text-align:center;font-size:11px}
        .stats-page .pie-content{display:grid;min-height:218px;grid-template-columns:minmax(130px,.9fr) minmax(0,1.1fr);align-items:center;gap:12px}
        .stats-page .pie-graphic{display:block;width:100%;max-width:210px;margin:auto;overflow:visible}
        .stats-page .pie-legend{display:grid;max-height:250px;align-content:center;gap:9px;overflow:auto}
        .stats-page .legend-item{display:grid;grid-template-columns:9px minmax(0,1fr);align-items:start;gap:8px;color:#687389;font-size:10px}
        .stats-page .legend-swatch{width:8px;height:8px;margin-top:3px;border-radius:3px}
        .stats-page .legend-main{min-width:0}.stats-page .legend-name{display:block;overflow-wrap:anywhere;color:#4b566c;font-weight:650}
        .stats-page .legend-meta{display:flex;flex-wrap:wrap;gap:5px;margin-top:3px;color:#929aaa;font-size:9px}
        .stats-page .legend-meta strong{color:#687389;font-weight:650}
        .stats-page .table-wrap{overflow-x:auto}
        .stats-page table{width:100%;min-width:420px;border-collapse:collapse}
        .stats-page th{padding:10px 9px;border-bottom:1px solid #eceef4;color:#8992a4;text-align:left;text-transform:uppercase;letter-spacing:.55px;font-size:8px;font-weight:750}
        .stats-page td{padding:11px 9px;border-bottom:1px solid #f0f1f5;color:#687389;font-size:10px}
        .stats-page tbody tr:last-child td{border-bottom:0}
        .stats-page .product-name{display:block;max-width:310px;overflow:hidden;color:#354057;font-weight:700;text-overflow:ellipsis;white-space:nowrap}
        .stats-page .product-rank{display:inline-grid;width:24px;height:24px;place-items:center;border-radius:7px;background:#f0efff;color:#665bd0;font-weight:750}
        .stats-page .revenue-cell{color:#354057;font-weight:750;white-space:nowrap}
        .stats-page .inline-bar{max-width:220px;height:4px;margin-top:6px;overflow:hidden;border-radius:99px;background:#f0f1f6}
        .stats-page .inline-bar span{display:block;height:100%;border-radius:99px;background:linear-gradient(90deg,#8277eb,#5c50ca)}
        .stats-page .operations-heading{display:flex;align-items:flex-end;justify-content:space-between;gap:10px;margin:21px 0 11px}
        .stats-page .operations-heading h2{margin:0;color:#283248;font:600 16px "Space Grotesk",sans-serif;letter-spacing:-.3px}
        .stats-page .operations-heading p{margin:5px 0 0;color:var(--muted);font-size:10px}
        .stats-page .operations-grid{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:10px}
        .stats-page .operation-card{display:flex;min-width:0;gap:9px;align-items:flex-start;padding:12px;border:1px solid var(--line);border-radius:11px;background:#fff;transition:border-color .16s,transform .16s,box-shadow .16s}
        .stats-page .operation-card:hover{transform:translateY(-1px);border-color:#c9c3f4;box-shadow:0 5px 13px #24213a09}
        .stats-page .operation-icon{display:grid;width:30px;height:30px;flex:0 0 30px;place-items:center;border-radius:8px;background:#f1efff;color:#675bd0;font-size:14px}
        .stats-page .operation-copy{min-width:0}
        .stats-page .operation-label{display:block;color:#727d91;font-size:9px;font-weight:650;line-height:1.45}
        .stats-page .operation-count{display:block;margin-top:5px;color:#263149;font:600 19px "Space Grotesk",sans-serif}
        .stats-page .operation-link{display:block;margin-top:3px;color:var(--violet);font-size:9px;font-weight:700}
        .stats-page .footnote{margin:15px 0 0;color:#929aaa;font-size:9px;line-height:1.65}
        .stats-page .skeleton{color:transparent!important;background:linear-gradient(90deg,#f2f3f8 20%,#e8ebf2 38%,#f2f3f8 60%);background-size:400% 100%;animation:stats-shimmer 1.2s ease infinite;border-radius:6px}
        @keyframes stats-shimmer{0%{background-position:100% 0}100%{background-position:0 0}}
        @media(max-width:1320px){.stats-page .operations-grid{grid-template-columns:repeat(3,minmax(0,1fr))}}
        @media(max-width:1080px){.stats-page .filters{grid-template-columns:repeat(2,minmax(0,1fr))}.stats-page .filter-apply{min-height:38px}.stats-page .charts-grid,.stats-page .charts-grid.equal{grid-template-columns:1fr}.stats-page .operations-grid{grid-template-columns:repeat(3,minmax(0,1fr))}}
        @media(max-width:820px){.stats-page .heading-row{align-items:flex-start}.stats-page .kpi-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.stats-page .operations-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}
        @media(max-width:600px){.stats-page .heading-row{display:block;margin-bottom:18px}.stats-page .heading-actions{margin-top:14px}.stats-page .heading-actions .button{flex:1}.stats-page .filter-panel{padding:13px}.stats-page .filter-top{align-items:flex-start;flex-direction:column;gap:5px}.stats-page .filters{grid-template-columns:repeat(2,minmax(0,1fr))}.stats-page .filter-apply{grid-column:1/-1}.stats-page .kpi-grid{gap:8px}.stats-page .kpi-card{padding:12px}.stats-page .kpi-value{font-size:19px}.stats-page .kpi-bottom{display:grid}.stats-page .panel{padding:13px}.stats-page .pie-content{grid-template-columns:1fr;gap:6px}.stats-page .pie-graphic{max-width:175px}.stats-page .pie-legend{grid-template-columns:repeat(2,minmax(0,1fr));max-height:none}.stats-page .operations-grid{grid-template-columns:repeat(2,minmax(0,1fr));gap:8px}.stats-page .operation-card{padding:10px 9px;gap:7px}.stats-page .operation-count{font-size:17px}.stats-page .operations-heading{align-items:flex-start;flex-direction:column}.stats-page .filter-helper{line-height:1.5}}
        @media(max-width:380px){.stats-page .filters,.stats-page .kpi-grid,.stats-page .operations-grid{grid-template-columns:1fr}.stats-page .heading-actions{flex-direction:column}.stats-page .heading-actions .button{width:100%}}
        @media print{.admin-sidebar,.admin-header,.stats-page .filter-panel,.stats-page .heading-actions,.page-footer{display:none!important}.admin-main{margin:0!important}.page-content{width:100%;padding:0!important}.stats-page .kpi-card,.stats-page .panel,.stats-page .operation-card{box-shadow:none;break-inside:avoid}.stats-page .charts-grid,.stats-page .charts-grid.equal{grid-template-columns:1fr 1fr}.stats-page .bar-viewport{overflow:visible}.stats-page .bar-viewport svg{max-width:100%;height:auto}}
    </style>
</head>
<body>
<div class="admin-shell">
    <aside class="admin-sidebar" id="admin-sidebar">
        <a class="brand" href="<%= contextPath %>/admin">
            <span class="brand-mark"><i></i><i></i><i></i></span>
            <span class="brand-copy"><strong>PrintKraft<span>.</span></strong><small>MARKETPLACE ADMIN</small></span>
        </a>
        <div class="workspace-switch"><span class="workspace-cube">▦</span>
            <span class="workspace-copy"><small>Quản trị vận hành</small><strong>Marketplace</strong></span>
        </div>
        <nav class="sidebar-nav" aria-label="Điều hướng chính">
            <div class="nav-group">
                <div class="nav-label">Quản lý marketplace</div>
                <a class="nav-item" href="<%= contextPath %>/admin"><span class="nav-icon">▦</span><span class="nav-text">Dashboard</span></a>
                <a class="nav-item" href="<%= contextPath %>/admin/users"><span class="nav-icon">♙</span><span class="nav-text">Người dùng</span></a>
                <a class="nav-item" href="<%= contextPath %>/admin/sellers"><span class="nav-icon">⌂</span><span class="nav-text">Seller / Xưởng in</span></a>
                <a class="nav-item" href="<%= contextPath %>/admin/products"><span class="nav-icon">◇</span><span class="nav-text">Sản phẩm</span></a>
                <a class="nav-item" href="<%= contextPath %>/admin/orders"><span class="nav-icon">▣</span><span class="nav-text">Đơn hàng</span></a>
                <a class="nav-item active" href="<%= contextPath %>/admin/statistics" aria-current="page"><span class="nav-icon">▥</span><span class="nav-text">Thống kê</span></a>
                <a class="nav-item" href="<%= contextPath %>/admin/tickets"><span class="nav-icon">☏</span><span class="nav-text">Hỗ trợ khách hàng</span></a>
            </div>
        </nav>
        <div class="sidebar-bottom">
            <div class="sidebar-admin"><span class="admin-avatar sidebar-avatar">A</span>
                <span class="sidebar-admin-copy"><strong>Không gian quản trị</strong><small>Quản trị viên</small></span>
            </div>
            <form class="logout-form" method="post" action="<%= contextPath %>/logout">
                <input type="hidden" name="csrfToken" value="<%= csrfToken %>">
                <button class="logout-button" type="submit" aria-label="Đăng xuất" title="Đăng xuất">↗<span>Đăng xuất</span></button>
            </form>
        </div>
    </aside>
    <main class="admin-main">
        <header class="admin-header">
            <div class="header-left">
                <button class="icon-button menu-toggle" type="button" data-action="toggle-sidebar" aria-label="Mở menu điều hướng" aria-controls="admin-sidebar" aria-expanded="false">☰</button>
                <div class="breadcrumbs"><span>PrintKraft</span><span>›</span><strong>Thống kê</strong></div>
            </div>
            <div class="header-actions">
                <span class="header-divider" aria-hidden="true"></span>
                <div class="admin-profile">
                    <span class="admin-avatar">A</span>
                    <span class="admin-profile-copy"><strong>Không gian quản trị</strong><small>Quản trị viên</small></span>
                </div>
            </div>
        </header>
        <div class="page-content">
            <div class="stats-page">
            <div class="heading-row">
                <div><div class="eyebrow">BUSINESS INTELLIGENCE</div><h1 class="page-title">Thống kê & phân tích</h1><p class="page-description">Theo dõi doanh thu, đơn hàng, khách hàng, sản phẩm, xưởng in và các hàng đợi vận hành trên cùng một màn hình.</p></div>
                <div class="heading-actions"><button class="button button-secondary" type="button" id="exportCsv">⇩ Xuất CSV</button><button class="button button-primary" type="button" id="printReport">▤ In báo cáo</button></div>
            </div>
            <form class="filter-panel" id="filterForm">
                <div class="filter-top"><span class="section-label">Bộ lọc báo cáo</span><span class="filter-helper">Chọn khoảng thời gian và cách gom nhóm để cập nhật tất cả biểu đồ.</span></div>
                <div class="filters">
                    <div class="field"><label for="fromDate">Từ ngày</label><input id="fromDate" name="from" type="date" value="<%= statsMonthStart %>" required></div>
                    <div class="field"><label for="toDate">Đến ngày</label><input id="toDate" name="to" type="date" value="<%= statsToday %>" required></div>
                    <div class="field"><label for="groupBy">Gom nhóm theo</label><select id="groupBy" name="groupBy"><option value="day" selected>Theo ngày</option><option value="month">Theo tháng</option><option value="year">Theo năm</option></select></div>
                    <div class="field"><label for="metric">Chỉ số biểu đồ cột</label><select id="metric" name="metric"><option value="revenue">Doanh thu đã thanh toán</option><option value="orders">Số đơn hàng</option><option value="customers">Khách hàng mới</option><option value="products">Sản phẩm mới</option><option value="shops">Xưởng in mới</option><option value="customRequests">Yêu cầu in 3D</option></select></div>
                    <button class="button button-primary filter-apply" id="applyFilters" type="submit">↻ Cập nhật báo cáo</button>
                </div>
                <div class="filter-buttons" aria-label="Khoảng thời gian nhanh">
                    <button type="button" class="preset-button active" data-preset="month">Tháng này</button>
                    <button type="button" class="preset-button" data-preset="7days">7 ngày qua</button>
                    <button type="button" class="preset-button" data-preset="30days">30 ngày qua</button>
                    <button type="button" class="preset-button" data-preset="year">Từ đầu năm</button>
                </div>
            </form>
            <div id="statusMessage" class="status-message" role="status" aria-live="polite"></div>
            <section class="kpi-grid" id="kpiGrid" aria-label="Chỉ số tổng quan"><article class="kpi-card"><div class="skeleton">Đang tải dữ liệu</div></article></section>
            <section class="charts-grid">
                <article class="panel">
                    <div class="panel-head"><div><h2 class="panel-title" id="trendTitle">Xu hướng doanh thu</h2><p class="panel-subtitle" id="trendSubtitle">So sánh theo ngày trong khoảng thời gian đã chọn.</p></div><span class="panel-tag" id="rangeTag">Tháng này</span></div>
                    <div id="trendChart" class="chart-empty">Đang tải biểu đồ…</div>
                </article>
                <article class="panel">
                    <div class="panel-head"><div><h2 class="panel-title">Cơ cấu trạng thái đơn hàng</h2><p class="panel-subtitle">Tỷ trọng đơn hàng theo trạng thái trong kỳ.</p></div><span class="panel-tag">Đơn hàng</span></div>
                    <div id="orderStatusChart" class="chart-empty">Đang tải biểu đồ…</div>
                </article>
            </section>
            <section class="charts-grid equal">
                <article class="panel">
                    <div class="panel-head"><div><h2 class="panel-title">Phương thức thanh toán</h2><p class="panel-subtitle">Phân bổ giao dịch theo phương thức ghi nhận trong kỳ.</p></div><span class="panel-tag">Thanh toán</span></div>
                    <div id="paymentChart" class="chart-empty">Đang tải biểu đồ…</div>
                </article>
                <article class="panel">
                    <div class="panel-head"><div><h2 class="panel-title">Doanh số theo danh mục</h2><p class="panel-subtitle">Phân bổ số lượng sản phẩm bán ra theo danh mục.</p></div><span class="panel-tag">Sản phẩm</span></div>
                    <div id="categoryChart" class="chart-empty">Đang tải biểu đồ…</div>
                </article>
            </section>
            <section class="panel">
                <div class="panel-head"><div><h2 class="panel-title">Sản phẩm nổi bật trong kỳ</h2><p class="panel-subtitle">Xếp theo doanh thu dòng hàng trong các đơn hàng đã tạo trong khoảng thời gian đã chọn.</p></div><span class="panel-tag">Top sản phẩm</span></div>
                <div class="table-wrap" id="topProductsTable"><div class="chart-empty">Đang tải danh sách sản phẩm…</div></div>
            </section>
            <div class="operations-heading"><div><h2>Hàng đợi vận hành</h2><p>Những nghiệp vụ cần theo dõi hoặc xử lý của quản trị viên.</p></div><span class="panel-tag">Trạng thái hiện tại</span></div>
            <section class="operations-grid" id="operationsGrid"><div class="operation-card"><div class="skeleton">Đang tải hàng đợi</div></div></section>
            <p class="footnote">Doanh thu trong báo cáo được tính từ đơn hàng có trạng thái thanh toán PAID, SUCCESS hoặc COMPLETED (hoặc có giao dịch thanh toán tương ứng đã thành công). Các ô hàng đợi vận hành phản ánh số lượng đang chờ xử lý hiện tại, không bị giới hạn bởi khoảng ngày đã chọn. Số liệu hiển thị phụ thuộc dữ liệu có trong cơ sở dữ liệu.</p>
            </div>
            <footer class="page-footer"><span>PrintKraft Marketplace</span><span>Admin</span></footer>
        </div>
    </main>
    <button class="sidebar-scrim" type="button" data-action="toggle-sidebar" aria-label="Đóng menu điều hướng"></button>
    <div id="modal-root"></div>
    <div id="toast-root" aria-live="polite" aria-atomic="true"></div>
</div>
<script>
(() => {
    const shell = document.querySelector('.admin-shell');
    const menuToggle = document.querySelector('.menu-toggle');
    const sidebarScrim = document.querySelector('.sidebar-scrim');
    function toggleSidebar() {
        if (!shell) return;
        const collapsed = shell.classList.toggle('sidebar-collapsed');
        menuToggle?.setAttribute('aria-expanded', String(collapsed));
        menuToggle?.setAttribute('aria-label', collapsed ? 'Đóng menu điều hướng' : 'Mở menu điều hướng');
    }
    menuToggle?.addEventListener('click', toggleSidebar);
    sidebarScrim?.addEventListener('click', toggleSidebar);

    const contextPath = "<%= contextPath %>";
    const form = document.getElementById('filterForm');
    const fromInput = document.getElementById('fromDate');
    const toInput = document.getElementById('toDate');
    const groupInput = document.getElementById('groupBy');
    const metricInput = document.getElementById('metric');
    const statusBox = document.getElementById('statusMessage');
    const colors = ['#7063dc','#3b8bdb','#1ea781','#eda947','#e56d7e','#27a6b8','#8a6ccf','#8e9bb1'];
    let latestData = null;
    let latestSeries = [];
    const metricInfo = {
        revenue: { title: 'Xu hướng doanh thu đã thanh toán', noun: 'Doanh thu', type: 'money' },
        orders: { title: 'Xu hướng số lượng đơn hàng', noun: 'Đơn hàng', type: 'count' },
        customers: { title: 'Xu hướng khách hàng mới', noun: 'Khách hàng mới', type: 'count' },
        products: { title: 'Xu hướng sản phẩm mới', noun: 'Sản phẩm mới', type: 'count' },
        shops: { title: 'Xu hướng xưởng in mới', noun: 'Xưởng in mới', type: 'count' },
        customRequests: { title: 'Xu hướng yêu cầu in 3D', noun: 'Yêu cầu in 3D', type: 'count' }
    };
    const queueInfo = [
        ['pendingOrders','Đơn hàng cần xử lý','▣','/admin/orders'],
        ['pendingShipments','Lô giao hàng chưa hoàn tất','↗','/admin/orders'],
        ['openSupportTickets','Ticket hỗ trợ đang mở','☏','/admin/tickets'],
        ['pendingCustomRequests','Yêu cầu in 3D chờ báo giá','◇','/admin/orders?view=quotes'],
        ['pendingQuotes','Báo giá B2B cần xử lý','▤','/admin/orders?view=quotes'],
        ['pendingProducts','Sản phẩm chờ duyệt','◈','/admin/products?status=PENDING_REVIEW'],
        ['pendingSellerVerifications','Xưởng chờ xác minh','⌂','/admin/sellers?verification=PENDING'],
        ['pendingChangeRequests','Yêu cầu đổi/hủy đơn','↺','/admin/orders'],
        ['refundRequired','Giao dịch cần hoàn tiền','₫','/admin/orders'],
        ['unpublishedReviews','Đánh giá chờ kiểm duyệt','☆','/admin/products']
    ];
    const numberFormat = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 });
    const moneyFormat = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 });
    function escapeHtml(value) { return String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c])); }
    function num(value) { const n = Number(value ?? 0); return Number.isFinite(n) ? n : 0; }
    function numberLabel(value) { return numberFormat.format(num(value)); }
    function moneyLabel(value) { return moneyFormat.format(num(value)); }
    function compactLabel(value) { const n = num(value); if (n >= 1000000000) return (n / 1000000000).toLocaleString('vi-VN',{maximumFractionDigits:1}) + ' tỷ'; if (n >= 1000000) return (n / 1000000).toLocaleString('vi-VN',{maximumFractionDigits:1}) + ' tr'; if (n >= 1000) return (n / 1000).toLocaleString('vi-VN',{maximumFractionDigits:1}) + ' k'; return numberLabel(n); }
    function showStatus(message, type) { statusBox.textContent = message || ''; statusBox.className = 'status-message' + (message ? ' show' : '') + (type ? ' ' + type : ''); }
    function parseDate(value) { const parts = value.split('-').map(Number); return new Date(parts[0], parts[1] - 1, parts[2]); }
    function dateString(date) { return date.getFullYear() + '-' + String(date.getMonth()+1).padStart(2,'0') + '-' + String(date.getDate()).padStart(2,'0'); }
    function periodKey(date, group) { if (group === 'year') return String(date.getFullYear()); if (group === 'month') return date.getFullYear() + '-' + String(date.getMonth()+1).padStart(2,'0'); return dateString(date); }
    function periodLabel(key, group) { if (group === 'year') return key; if (group === 'month') { const [y,m] = key.split('-'); return m + '/' + y; } const [y,m,d] = key.split('-'); return d + '/' + m; }
    function buildBuckets(fromText, toText, group, values) {
        const start = parseDate(fromText); const finish = parseDate(toText); const map = new Map((values || []).map(row => [String(row.period), num(row.value)]));
        const list = []; const cur = new Date(start.getFullYear(), start.getMonth(), start.getDate());
        if (group === 'month') cur.setDate(1); if (group === 'year') { cur.setMonth(0,1); }
        while (cur <= finish && list.length < 1500) {
            const key = periodKey(cur, group); list.push({ key, label: periodLabel(key, group), value: map.get(key) || 0 });
            if (group === 'year') cur.setFullYear(cur.getFullYear()+1,0,1); else if (group === 'month') cur.setMonth(cur.getMonth()+1,1); else cur.setDate(cur.getDate()+1);
        }
        return list;
    }
    function changeText(current, previous) {
        const c = num(current), p = num(previous); if (p === 0 && c === 0) return {text:'Không đổi', tone:'neutral'}; if (p === 0) return {text:'Mới trong kỳ', tone:'up'};
        const delta = (c-p)/Math.abs(p)*100; return {text:(delta > 0 ? '+' : '') + delta.toLocaleString('vi-VN',{maximumFractionDigits:1}) + '% so với kỳ trước', tone:delta > 0 ? 'up' : delta < 0 ? 'down' : 'neutral'};
    }
    function card(title, value, sub, icon, tone, change) {
        return `<article class="kpi-card kpi-${tone}"><div class="kpi-head"><span>${escapeHtml(title)}</span><span class="kpi-icon">${icon}</span></div><div class="kpi-value" title="${escapeHtml(value)}">${escapeHtml(value)}</div><div class="kpi-bottom"><span class="kpi-change ${change?.tone || 'neutral'}">${escapeHtml(change?.text || '')}</span><span>${escapeHtml(sub)}</span></div><i class="kpi-decoration" aria-hidden="true"></i></article>`;
    }
    function renderKpis(summary, previous, data) {
        const orderChange = changeText(summary.orderCount, previous.orderCount);
        const revenueChange = changeText(summary.revenue, previous.revenue);
        const customerChange = changeText(summary.newCustomers, previous.newCustomers);
        const paidRate = num(summary.orderCount) ? num(summary.paidOrderCount) / num(summary.orderCount) * 100 : 0;
        const cancelRate = num(summary.orderCount) ? num(summary.cancelledOrderCount) / num(summary.orderCount) * 100 : 0;
        const cards = [
            card('Doanh thu đã thanh toán', moneyLabel(summary.revenue), 'So với kỳ liền trước', '₫', 'purple', revenueChange),
            card('Tổng đơn hàng', numberLabel(summary.orderCount), 'Đơn phát sinh trong kỳ', '▣', 'blue', orderChange),
            card('Giá trị đơn trung bình', moneyLabel(summary.averageOrderValue), 'Trên đơn đã thanh toán', '↗', 'green', {text:numberLabel(summary.paidOrderCount)+' đơn đã thanh toán',tone:'neutral'}),
            card('Khách hàng mới', numberLabel(summary.newCustomers), 'Tài khoản khách trong kỳ', '♙', 'cyan', customerChange),
            card('Sản phẩm mới', numberLabel(summary.newProducts), 'Sản phẩm tạo trong kỳ', '◇', 'amber', {text:numberLabel(summary.newShops)+' xưởng in mới',tone:'neutral'}),
            card('Tỷ lệ thanh toán', paidRate.toLocaleString('vi-VN',{maximumFractionDigits:1})+'%', 'Tỷ lệ trên đơn phát sinh', '✓', 'rose', {text:'Tỷ lệ hủy '+cancelRate.toLocaleString('vi-VN',{maximumFractionDigits:1})+'%',tone:'neutral'})
        ];
        document.getElementById('kpiGrid').innerHTML = cards.join('');
    }
    function renderBarChart(rows, group) {
        const holder = document.getElementById('trendChart');
        if (!rows.length) { holder.className='chart-empty'; holder.textContent='Không có dữ liệu trong khoảng thời gian này.'; return; }
        holder.className='bar-viewport';
        const maxValue = Math.max(1, ...rows.map(row => row.value));
        const gap = rows.length > 70 ? 7 : rows.length > 35 ? 10 : rows.length > 18 ? 14 : 21;
        const barWidth = Math.max(5, Math.min(25, gap * .58));
        const left=60, right=15, top=18, bottom=53, height=290;
        const plotHeight=height-top-bottom, plotWidth=rows.length*gap;
        const width=left+plotWidth+right;
        const axes=[];
        for(let i=0;i<=4;i++){ const y=top+plotHeight*i/4; const v=maxValue*(1-i/4); axes.push(`<line x1="${left}" y1="${y}" x2="${left+plotWidth}" y2="${y}" stroke="#e9edf5" stroke-dasharray="3 4"/><text x="${left-10}" y="${y+4}" text-anchor="end" font-size="10" fill="#8993a7">${escapeHtml(metricInput.value==='revenue'?compactLabel(v):numberLabel(Math.round(v)))}</text>`); }
        const every=Math.max(1,Math.ceil(rows.length/14));
        const bars=rows.map((row,index)=>{const x=left+index*gap+(gap-barWidth)/2;const bh=num(row.value)/maxValue*plotHeight;const y=top+plotHeight-bh;const show=index%every===0||index===rows.length-1;return `<g><title>${escapeHtml(row.label)}: ${escapeHtml(metricInput.value==='revenue'?moneyLabel(row.value):numberLabel(row.value))}</title><rect x="${x}" y="${y}" width="${barWidth}" height="${Math.max(0,bh)}" rx="${Math.min(4,barWidth/3)}" fill="#7063dc" opacity="${num(row.value)===0?'.18':'.92'}"/><rect x="${x}" y="${Math.max(top,y)}" width="${barWidth}" height="${Math.min(5,bh)}" rx="2" fill="#a69eff"/>${show?`<text x="${x+barWidth/2}" y="${height-28}" text-anchor="middle" font-size="10" fill="#7d879b">${escapeHtml(row.label)}</text>`:''}</g>`;}).join('');
        holder.innerHTML=`<svg role="img" aria-label="Biểu đồ cột ${escapeHtml(metricInfo[metricInput.value].noun)}" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">${axes.join('')}${bars}<text x="${left+plotWidth/2}" y="${height-7}" text-anchor="middle" font-size="10" fill="#a0a7b5">${group==='day'?'Ngày':group==='month'?'Tháng':'Năm'}</text></svg>`;
    }
    function sectorPath(cx,cy,r,start,end) {
        if (end-start >= 359.99) return `M ${cx} ${cy-r} A ${r} ${r} 0 1 1 ${cx} ${cy+r} A ${r} ${r} 0 1 1 ${cx} ${cy-r} Z`;
        const rad=Math.PI/180; const x1=cx+r*Math.cos(start*rad), y1=cy+r*Math.sin(start*rad); const x2=cx+r*Math.cos(end*rad), y2=cy+r*Math.sin(end*rad); const large=end-start>180?1:0;
        return `M ${cx} ${cy} L ${x1.toFixed(2)} ${y1.toFixed(2)} A ${r} ${r} 0 ${large} 1 ${x2.toFixed(2)} ${y2.toFixed(2)} Z`;
    }
    function renderPie(holderId, items, valueKey='value', metaKey=null, metaType='count') {
        const holder=document.getElementById(holderId); const clean=(items||[]).map(item=>({label:String(item.label||'Chưa phân loại'),value:Math.max(0,num(item[valueKey])),meta:metaKey?num(item[metaKey]):null})).filter(item=>item.value>0);
        if(!clean.length){holder.className='chart-empty';holder.textContent='Chưa có dữ liệu để phân bổ trong kỳ.';return;}
        holder.className='pie-content';const total=clean.reduce((sum,item)=>sum+item.value,0);let offset=-90;
        const paths=clean.map((item,index)=>{const angle=item.value/total*360;const path=sectorPath(100,100,86,offset,offset+angle);const svg=`<path d="${path}" fill="${colors[index%colors.length]}" stroke="#fff" stroke-width="2"><title>${escapeHtml(item.label)}: ${numberLabel(item.value)}</title></path>`;offset+=angle;return svg;}).join('');
        const legend=clean.map((item,index)=>{const share=item.value/total*100;let extra=numberLabel(item.value)+' ('+share.toLocaleString('vi-VN',{maximumFractionDigits:1})+'%)';if(item.meta!==null)extra+= ' · '+(metaType==='money'?moneyLabel(item.meta):numberLabel(item.meta));return `<div class="legend-item"><span class="legend-swatch" style="background:${colors[index%colors.length]}"></span><span class="legend-main"><span class="legend-name">${escapeHtml(item.label)}</span><span class="legend-meta"><strong>${escapeHtml(extra)}</strong></span></span></div>`;}).join('');
        holder.innerHTML=`<svg class="pie-graphic" viewBox="0 0 200 200" role="img" aria-label="Biểu đồ tròn phân bổ">${paths}</svg><div class="pie-legend">${legend}</div>`;
    }
    function renderProducts(items) {
        const host=document.getElementById('topProductsTable');
        if(!items||!items.length){host.innerHTML='<div class="chart-empty">Chưa có sản phẩm phát sinh đơn hàng trong khoảng thời gian này.</div>';return;}
        const max=Math.max(1,...items.map(item=>num(item.revenue)));
        host.innerHTML=`<table><thead><tr><th>#</th><th>Sản phẩm</th><th>Số lượng</th><th>Doanh thu dòng hàng</th></tr></thead><tbody>${items.map((item,index)=>`<tr><td><span class="product-rank">${index+1}</span></td><td><span class="product-name" title="${escapeHtml(item.name)}">${escapeHtml(item.name||'Sản phẩm')}</span><div class="inline-bar"><span style="width:${Math.max(0,Math.min(100,num(item.revenue)/max*100))}%"></span></div></td><td>${numberLabel(item.quantity)}</td><td class="revenue-cell">${moneyLabel(item.revenue)}</td></tr>`).join('')}</tbody></table>`;
    }
    function renderOperations(operations) {
        document.getElementById('operationsGrid').innerHTML=queueInfo.map(([key,label,icon,href])=>`<a class="operation-card" href="${contextPath}${href}"><span class="operation-icon">${icon}</span><span class="operation-copy"><span class="operation-label">${escapeHtml(label)}</span><strong class="operation-count">${numberLabel(operations[key])}</strong><span class="operation-link">Mở nghiệp vụ →</span></span></a>`).join('');
    }
    function renderAll(data) {
        latestData=data;
        const metric=metricInfo[metricInput.value]||metricInfo.revenue;
        const group=groupInput.value;
        const from=fromInput.value,to=toInput.value;
        const summary=data.summary||{}, previous=data.previous||{};
        renderKpis(summary,previous,data);
        const trendMap=data.trends?.[metricInput.value]||[];
        const trend=buildBuckets(from,to,group,trendMap);
        latestSeries=trend;
        document.getElementById('trendTitle').textContent=metric.title;
        document.getElementById('trendSubtitle').textContent=(group==='day'?'Theo ngày':group==='month'?'Theo tháng':'Theo năm')+' · '+from+' đến '+to;
        document.getElementById('rangeTag').textContent=group==='day'?'Theo ngày':group==='month'?'Theo tháng':'Theo năm';
        renderBarChart(trend,group);
        renderPie('orderStatusChart',data.orderStatuses||[],'value');
        renderPie('paymentChart',data.paymentMethods||[],'value','amount','money');
        renderPie('categoryChart',data.categorySales||[],'value','amount','money');
        renderProducts(data.topProducts||[]);
        renderOperations(data.operations||{});
        showStatus('Đã cập nhật báo cáo: '+from+' → '+to+'.','success');
        document.querySelectorAll('[data-preset]').forEach(button=>button.classList.remove('active'));
    }
    async function loadData() {
        if(!fromInput.value||!toInput.value){showStatus('Vui lòng chọn ngày bắt đầu và ngày kết thúc.','error');return;}
        if(fromInput.value>toInput.value){showStatus('Ngày bắt đầu không được sau ngày kết thúc.','error');return;}
        const apply=document.getElementById('applyFilters');apply.disabled=true;showStatus('Đang lấy số liệu mới nhất từ cơ sở dữ liệu…','loading');
        try {
            const params=new URLSearchParams({from:fromInput.value,to:toInput.value,groupBy:groupInput.value});
            const response=await fetch(`${contextPath}/admin/api/statistics?${params.toString()}`,{credentials:'same-origin',headers:{'Accept':'application/json'}});
            let payload;try{payload=await response.json();}catch{throw new Error('Máy chủ trả về dữ liệu không hợp lệ.');}
            if(!response.ok)throw new Error(payload.error||'Không thể tải báo cáo.');
            renderAll(payload.data||{});
        } catch(error) { showStatus(error.message||'Không thể tải số liệu. Hãy kiểm tra kết nối cơ sở dữ liệu và thử lại.','error'); }
        finally { apply.disabled=false; }
    }
    function applyPreset(preset) {
        const today=parseDate(toInput.value||dateString(new Date()));let start=new Date(today);let group='day';
        if(preset==='month'){start=new Date(today.getFullYear(),today.getMonth(),1);group='day';}
        else if(preset==='7days'){start.setDate(start.getDate()-6);group='day';}
        else if(preset==='30days'){start.setDate(start.getDate()-29);group='day';}
        else if(preset==='year'){start=new Date(today.getFullYear(),0,1);group='month';}
        fromInput.value=dateString(start);toInput.value=dateString(today);groupInput.value=group;
        document.querySelectorAll('[data-preset]').forEach(button=>button.classList.toggle('active',button.dataset.preset===preset));
        loadData();
    }
    function csvCell(value) { let text=String(value??''); if(/^[=+\-@\t\r]/.test(text))text="'"+text;return '"'+text.replace(/"/g,'""')+'"'; }
    function exportCsv() {
        if(!latestData)return;const s=latestData.summary||{},ops=latestData.operations||{};const records=[
            ['Báo cáo thống kê PrintKraft'],['Từ ngày',fromInput.value],['Đến ngày',toInput.value],['Gom nhóm',groupInput.options[groupInput.selectedIndex].text],[],
            ['Chỉ số','Giá trị'],['Doanh thu đã thanh toán',s.revenue||0],['Đơn hàng',s.orderCount||0],['Đơn đã thanh toán',s.paidOrderCount||0],['Giá trị đơn trung bình',s.averageOrderValue||0],['Khách hàng mới',s.newCustomers||0],['Sản phẩm mới',s.newProducts||0],['Xưởng in mới',s.newShops||0],[],
            ['Xu hướng',metricInfo[metricInput.value].noun,'Giá trị'],...latestSeries.map(item=>[item.label,metricInfo[metricInput.value].noun,item.value]),[],
            ['Hàng đợi vận hành','Số lượng'],...queueInfo.map(([key,label])=>[label,ops[key]||0]),[],
            ['Sản phẩm nổi bật','Số lượng','Doanh thu'],...(latestData.topProducts||[]).map(item=>[item.name,item.quantity,item.revenue])
        ];const csv='\uFEFF'+records.map(row=>row.map(csvCell).join(',')).join('\r\n');const blob=new Blob([csv],{type:'text/csv;charset=utf-8;'});const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download=`printkraft-thong-ke-${fromInput.value}-${toInput.value}.csv`;document.body.appendChild(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(url),0);
    }
    form.addEventListener('submit',event=>{event.preventDefault();loadData();});
    document.querySelectorAll('[data-preset]').forEach(button=>button.addEventListener('click',()=>applyPreset(button.dataset.preset)));
    metricInput.addEventListener('change',()=>{if(latestData)renderAll(latestData);});
    document.getElementById('exportCsv').addEventListener('click',exportCsv);
    document.getElementById('printReport').addEventListener('click',()=>window.print());
    loadData();
})();
</script>
</body>
</html>